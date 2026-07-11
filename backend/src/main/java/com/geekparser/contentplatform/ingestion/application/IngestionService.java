package com.geekparser.contentplatform.ingestion.application;

import com.geekparser.contentplatform.article.application.LlmRewriteService;
import com.geekparser.contentplatform.article.application.MarkdownStorageService;
import com.geekparser.contentplatform.article.domain.ArticleStatus;
import com.geekparser.contentplatform.article.domain.PreparedArticle;
import com.geekparser.contentplatform.article.domain.PreparedArticleRepository;
import com.geekparser.contentplatform.article.domain.RawArticle;
import com.geekparser.contentplatform.article.domain.RawArticleRepository;
import com.geekparser.contentplatform.ingestion.domain.Source;
import com.geekparser.contentplatform.ingestion.domain.SourceRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);
    private static final String PROCESSING_FAILURE_MESSAGE = "LLM translation/summarization failed. Article requires manual retry after LLM setup is fixed.";
    private static final int TEST_SYNC_LIMIT = 5;

    private final SourceRepository sourceRepository;
    private final RawArticleRepository rawArticleRepository;
    private final PreparedArticleRepository preparedArticleRepository;
    private final SourceScraper sourceScraper;
    private final MarkdownStorageService markdownStorageService;
    private final LlmRewriteService llmRewriteService;
    private final Clock clock;

    public IngestionService(SourceRepository sourceRepository,
                            RawArticleRepository rawArticleRepository,
                            PreparedArticleRepository preparedArticleRepository,
                            SourceScraper sourceScraper,
                            MarkdownStorageService markdownStorageService,
                            LlmRewriteService llmRewriteService,
                            Clock clock) {
        this.sourceRepository = sourceRepository;
        this.rawArticleRepository = rawArticleRepository;
        this.preparedArticleRepository = preparedArticleRepository;
        this.sourceScraper = sourceScraper;
        this.markdownStorageService = markdownStorageService;
        this.llmRewriteService = llmRewriteService;
        this.clock = clock;
    }

    @Scheduled(cron = "${app.scheduling.ingestion-cron}")
    public void scheduledSync() {
        syncLast48Hours();
    }

    @Transactional
    public SyncReport syncLast48Hours() {
        OffsetDateTime cutoff = OffsetDateTime.now(clock).minusHours(48);
        List<Source> sources = sourceRepository.findByActiveTrueOrderByNameAsc();
        int discovered = 0;
        int stored = 0;
        for (Source source : sources) {
            try {
                List<FetchedArticle> articles = sourceScraper.fetchArticlesSince(source, cutoff);
                discovered += articles.size();
                for (FetchedArticle article : articles) {
                    if (rawArticleRepository.findByCanonicalUrl(article.canonicalUrl()).isPresent()) {
                        continue;
                    }
                    storeArticle(source, article);
                    stored++;
                    if (stored >= TEST_SYNC_LIMIT) {
                        return new SyncReport(discovered, stored);
                    }
                }
            } catch (Exception exception) {
                log.warn("Failed to sync source {}: {}", source.getCode(), exception.getMessage());
            }
        }
        return new SyncReport(discovered, stored);
    }

    private void storeArticle(Source source, FetchedArticle fetchedArticle) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        String slug = slugify(fetchedArticle.title());
        String markdownPath = markdownStorageService.saveRawArticle(
                source,
                slug,
                fetchedArticle.title(),
                fetchedArticle.canonicalUrl(),
                fetchedArticle.publishedAt(),
                fetchedArticle.body()
        );

        RawArticle rawArticle = new RawArticle();
        rawArticle.setSource(source);
        rawArticle.setCanonicalUrl(fetchedArticle.canonicalUrl());
        rawArticle.setTitle(fetchedArticle.title());
        rawArticle.setSlug(slug);
        rawArticle.setContentHash(hash(fetchedArticle.body()));
        rawArticle.setStatus(ArticleStatus.AI_PROCESSING);
        rawArticle.setPublishedAt(fetchedArticle.publishedAt());
        rawArticle.setMarkdownPath(markdownPath);
        rawArticle.setRawExcerpt(fetchedArticle.body().substring(0, Math.min(fetchedArticle.body().length(), 600)));
        rawArticle.setSourcePayload(fetchedArticle.body());
        rawArticle.setCreatedAt(now);
        rawArticle.setUpdatedAt(now);
        RawArticle saved = rawArticleRepository.save(rawArticle);

        try {
            LlmRewriteService.RewriteResult rewriteResult = llmRewriteService.rewrite(saved.getTitle(), fetchedArticle.body());
            PreparedArticle preparedArticle = new PreparedArticle();
            preparedArticle.setRawArticle(saved);
            preparedArticle.setTitle(rewriteResult.title());
            preparedArticle.setTranslatedBody(rewriteResult.translatedBody());
            preparedArticle.setSummaryBody(rewriteResult.summaryBody());
            preparedArticle.setEditorialNotes("");
            preparedArticle.setNeedsManualReview(rewriteResult.needsManualReview());
            preparedArticle.setStatus(rewriteResult.needsManualReview() ? ArticleStatus.READY_FOR_REVIEW : ArticleStatus.READY_TO_PUBLISH);
            preparedArticle.setLlmModel(rewriteResult.model());
            preparedArticle.setCreatedAt(now);
            preparedArticle.setUpdatedAt(now);
            preparedArticleRepository.save(preparedArticle);

            saved.setStatus(preparedArticle.getStatus());
            saved.setUpdatedAt(OffsetDateTime.now(clock));
        } catch (Exception exception) {
            log.warn("LLM processing failed for article {}: {}", saved.getCanonicalUrl(), exception.getMessage());
            saved.setStatus(ArticleStatus.FAILED);
            saved.setUpdatedAt(OffsetDateTime.now(clock));

            PreparedArticle failedArticle = new PreparedArticle();
            failedArticle.setRawArticle(saved);
            failedArticle.setTitle("Требуется повторная AI-обработка");
            failedArticle.setTranslatedBody("Автоматический перевод не был выполнен.");
            failedArticle.setSummaryBody("Публикация не готова: LLM не вернул корректный русский summary.");
            failedArticle.setEditorialNotes(PROCESSING_FAILURE_MESSAGE + " Причина: " + exception.getMessage());
            failedArticle.setNeedsManualReview(true);
            failedArticle.setStatus(ArticleStatus.FAILED);
            failedArticle.setLlmModel(llmRewriteService.getConfiguredModel());
            failedArticle.setCreatedAt(now);
            failedArticle.setUpdatedAt(OffsetDateTime.now(clock));
            preparedArticleRepository.save(failedArticle);
        }
    }

    private String slugify(String title) {
        String slug = title.toLowerCase()
                .replaceAll("[^a-zа-я0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return slug.isBlank() ? "article-" + System.currentTimeMillis() : slug.substring(0, Math.min(slug.length(), 120));
    }

    private String hash(String body) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to hash article body", exception);
        }
    }

    public record SyncReport(int discovered, int stored) {
    }
}
