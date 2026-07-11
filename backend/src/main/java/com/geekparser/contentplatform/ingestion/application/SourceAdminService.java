package com.geekparser.contentplatform.ingestion.application;

import com.geekparser.contentplatform.admin.api.AdminDtos;
import com.geekparser.contentplatform.article.application.ArticleStorageCleanupService;
import com.geekparser.contentplatform.article.domain.RawArticle;
import com.geekparser.contentplatform.article.domain.RawArticleRepository;
import com.geekparser.contentplatform.ingestion.domain.Source;
import com.geekparser.contentplatform.ingestion.domain.SourceRepository;
import com.geekparser.contentplatform.ingestion.domain.SourceSeedExclusion;
import com.geekparser.contentplatform.ingestion.domain.SourceSeedExclusionRepository;
import com.geekparser.contentplatform.ingestion.domain.SourceType;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SourceAdminService {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[a-z][a-z0-9_-]{1,62}$");

    private final SourceRepository sourceRepository;
    private final RawArticleRepository rawArticleRepository;
    private final SourceProfileCatalog sourceProfileCatalog;
    private final ArticleStorageCleanupService articleStorageCleanupService;
    private final SourceSeedExclusionRepository sourceSeedExclusionRepository;
    private final Clock clock;

    public SourceAdminService(SourceRepository sourceRepository,
                              RawArticleRepository rawArticleRepository,
                              SourceProfileCatalog sourceProfileCatalog,
                              ArticleStorageCleanupService articleStorageCleanupService,
                              SourceSeedExclusionRepository sourceSeedExclusionRepository,
                              Clock clock) {
        this.sourceRepository = sourceRepository;
        this.rawArticleRepository = rawArticleRepository;
        this.sourceProfileCatalog = sourceProfileCatalog;
        this.articleStorageCleanupService = articleStorageCleanupService;
        this.sourceSeedExclusionRepository = sourceSeedExclusionRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<AdminDtos.SourceResponse> listSources() {
        return sourceRepository.findAllByOrderByNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AdminDtos.SourceResponse createSource(AdminDtos.CreateSourceRequest request) {
        validateCode(request.code());
        if (sourceRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("Source code already exists: " + request.code());
        }
        validateScrapingConfig(request.articleUrlPatterns());

        OffsetDateTime now = OffsetDateTime.now(clock);
        Source source = new Source();
        applyRequest(source, request);
        source.setCreatedAt(now);
        source.setUpdatedAt(now);
        Source saved = sourceRepository.save(source);
        sourceProfileCatalog.resolveProfile(saved);
        return toResponse(saved);
    }

    @Transactional
    public AdminDtos.SourceResponse updateSource(Long id, AdminDtos.UpdateSourceRequest request) {
        Source source = sourceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Source not found"));
        validateScrapingConfig(request.articleUrlPatterns());
        applyRequest(source, request);
        source.setUpdatedAt(OffsetDateTime.now(clock));
        sourceProfileCatalog.resolveProfile(source);
        return toResponse(sourceRepository.save(source));
    }

    @Transactional
    public void deleteSource(Long id, boolean deleteArticles) {
        Source source = sourceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Source not found"));
        List<RawArticle> linkedArticles = rawArticleRepository.findBySourceId(source.getId());
        if (!linkedArticles.isEmpty() && !deleteArticles) {
            throw new IllegalArgumentException(
                    "Source has linked articles. Pass deleteArticles=true or clear storage first."
            );
        }
        if (!linkedArticles.isEmpty()) {
            articleStorageCleanupService.deleteArticlesForSource(source.getId());
        }
        if (sourceProfileCatalog.isBuiltIn(source.getCode())
                && !sourceSeedExclusionRepository.existsById(source.getCode())) {
            sourceSeedExclusionRepository.save(
                    new SourceSeedExclusion(source.getCode(), OffsetDateTime.now(clock)));
        }
        sourceRepository.delete(source);
    }

    private void applyRequest(Source source, AdminDtos.CreateSourceRequest request) {
        source.setCode(normalizeCode(request.code()));
        source.setName(request.name().trim());
        source.setType(request.type());
        source.setBaseUrl(request.baseUrl().trim());
        source.setListingUrl(request.listingUrl().trim());
        source.setRssUrl(blankToNull(request.rssUrl()));
        source.setActive(request.active());
        source.setArticleUrlPatterns(request.articleUrlPatterns().trim());
        source.setBodySelectors(defaultSelectors(request.bodySelectors(), SourceProfileCatalog.DEFAULT_BODY_SELECTORS));
        source.setTitleSelectors(defaultSelectors(request.titleSelectors(), SourceProfileCatalog.DEFAULT_TITLE_SELECTORS));
        source.setPublishedAtSelectors(defaultSelectors(
                request.publishedAtSelectors(),
                SourceProfileCatalog.DEFAULT_PUBLISHED_AT_SELECTORS
        ));
    }

    private void applyRequest(Source source, AdminDtos.UpdateSourceRequest request) {
        source.setName(request.name().trim());
        source.setType(request.type());
        source.setBaseUrl(request.baseUrl().trim());
        source.setListingUrl(request.listingUrl().trim());
        source.setRssUrl(blankToNull(request.rssUrl()));
        source.setActive(request.active());
        source.setArticleUrlPatterns(request.articleUrlPatterns().trim());
        source.setBodySelectors(defaultSelectors(request.bodySelectors(), SourceProfileCatalog.DEFAULT_BODY_SELECTORS));
        source.setTitleSelectors(defaultSelectors(request.titleSelectors(), SourceProfileCatalog.DEFAULT_TITLE_SELECTORS));
        source.setPublishedAtSelectors(defaultSelectors(
                request.publishedAtSelectors(),
                SourceProfileCatalog.DEFAULT_PUBLISHED_AT_SELECTORS
        ));
    }

    private AdminDtos.SourceResponse toResponse(Source source) {
        return new AdminDtos.SourceResponse(
                source.getId(),
                source.getCode(),
                source.getName(),
                source.getType(),
                source.getBaseUrl(),
                source.getListingUrl(),
                source.getRssUrl(),
                source.isActive(),
                sourceProfileCatalog.isBuiltIn(source.getCode()),
                source.getArticleUrlPatterns(),
                source.getBodySelectors(),
                source.getTitleSelectors(),
                source.getPublishedAtSelectors(),
                source.getCreatedAt(),
                source.getUpdatedAt()
        );
    }

    private static void validateCode(String code) {
        String normalized = normalizeCode(code);
        if (!CODE_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "Source code must match pattern [a-z][a-z0-9_-]{1,62}, got: " + code
            );
        }
    }

    private static void validateScrapingConfig(String articleUrlPatterns) {
        if (SourceProfileCatalog.parsePatterns(articleUrlPatterns).isEmpty()) {
            throw new IllegalArgumentException("At least one article URL regex pattern is required.");
        }
    }

    private static String normalizeCode(String code) {
        return code.trim().toLowerCase();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String defaultSelectors(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
