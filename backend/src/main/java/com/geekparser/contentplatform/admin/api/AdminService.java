package com.geekparser.contentplatform.admin.api;

import com.geekparser.contentplatform.article.application.ArticleStorageCleanupService;
import com.geekparser.contentplatform.article.domain.ArticleStatus;
import com.geekparser.contentplatform.article.domain.PreparedArticle;
import com.geekparser.contentplatform.article.domain.PreparedArticleRepository;
import com.geekparser.contentplatform.ingestion.application.IngestionService;
import com.geekparser.contentplatform.ingestion.application.SourceAdminService;
import com.geekparser.contentplatform.publishing.application.PublishingService;
import com.geekparser.contentplatform.publishing.application.TelegramDraftService;
import com.geekparser.contentplatform.publishing.application.TelegramGateway;
import com.geekparser.contentplatform.publishing.application.TelegramSettingsService;
import com.geekparser.contentplatform.publishing.domain.PublicationJob;
import com.geekparser.contentplatform.publishing.domain.PublicationJobRepository;
import com.geekparser.contentplatform.publishing.domain.TelegramConfig;
import com.geekparser.contentplatform.publishing.domain.TelegramConfigRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private final PreparedArticleRepository preparedArticleRepository;
    private final PublicationJobRepository publicationJobRepository;
    private final TelegramConfigRepository telegramConfigRepository;
    private final IngestionService ingestionService;
    private final PublishingService publishingService;
    private final TelegramGateway telegramGateway;
    private final TelegramSettingsService telegramSettingsService;
    private final TelegramDraftService telegramDraftService;
    private final SourceAdminService sourceAdminService;
    private final ArticleStorageCleanupService articleStorageCleanupService;
    private final Clock clock;

    public AdminService(PreparedArticleRepository preparedArticleRepository,
                        PublicationJobRepository publicationJobRepository,
                        TelegramConfigRepository telegramConfigRepository,
                        IngestionService ingestionService,
                        PublishingService publishingService,
                        TelegramGateway telegramGateway,
                        TelegramSettingsService telegramSettingsService,
                        TelegramDraftService telegramDraftService,
                        SourceAdminService sourceAdminService,
                        ArticleStorageCleanupService articleStorageCleanupService,
                        Clock clock) {
        this.preparedArticleRepository = preparedArticleRepository;
        this.publicationJobRepository = publicationJobRepository;
        this.telegramConfigRepository = telegramConfigRepository;
        this.ingestionService = ingestionService;
        this.publishingService = publishingService;
        this.telegramGateway = telegramGateway;
        this.telegramSettingsService = telegramSettingsService;
        this.telegramDraftService = telegramDraftService;
        this.sourceAdminService = sourceAdminService;
        this.articleStorageCleanupService = articleStorageCleanupService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<AdminDtos.ArticleSummaryResponse> listArticles() {
        return preparedArticleRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminDtos.ArticleDetailsResponse getArticle(Long id) {
        return preparedArticleRepository.findById(id)
                .map(this::toDetails)
                .orElseThrow(() -> new IllegalArgumentException("Article not found"));
    }

    @Transactional
    public AdminDtos.ArticleDetailsResponse updateArticle(Long id, AdminDtos.UpdateArticleRequest request) {
        PreparedArticle article = preparedArticleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Article not found"));
        article.setTitle(request.title());
        article.setSummaryBody(request.summaryBody());
        article.setTranslatedBody(request.translatedBody());
        article.setEditorialNotes(request.editorialNotes());
        article.setStatus(request.readyToPublish() ? ArticleStatus.READY_TO_PUBLISH : ArticleStatus.READY_FOR_REVIEW);
        article.setNeedsManualReview(!request.readyToPublish());
        article.setUpdatedAt(OffsetDateTime.now(clock));
        return toDetails(preparedArticleRepository.save(article));
    }

    @Transactional
    public AdminDtos.PublicationResponse schedule(Long id, AdminDtos.SchedulePublicationRequest request) {
        return toPublication(publishingService.schedule(id, request.scheduledAt()));
    }

    @Transactional
    public AdminDtos.PublicationResponse publishNow(Long id) {
        return toPublication(publishingService.publishNow(id));
    }

    @Transactional
    public AdminDtos.SendDraftResponse sendDraft(Long id) {
        TelegramDraftService.DraftSendResult result = telegramDraftService.sendDraft(id);
        return new AdminDtos.SendDraftResponse(result.messageId(), result.draftChatId());
    }

    @Transactional(readOnly = true)
    public List<AdminDtos.DiscoveredChatResponse> discoverTelegramChats(String botToken) {
        return telegramGateway.discoverRecentPrivateChats(botToken).stream()
                .map(chat -> new AdminDtos.DiscoveredChatResponse(
                        chat.chatId(),
                        chat.username(),
                        chat.firstName(),
                        chat.lastName(),
                        chat.lastMessageText()
                ))
                .toList();
    }

    public String requireBotTokenForDiscovery() {
        return telegramSettingsService.requireActiveCredentials().botToken();
    }

    @Transactional(readOnly = true)
    public List<AdminDtos.PublicationResponse> listPublications() {
        return publicationJobRepository.findAllByOrderByScheduledAtDesc().stream()
                .map(this::toPublication)
                .toList();
    }

    @Transactional
    public AdminDtos.SyncResponse syncSources() {
        IngestionService.SyncReport report = ingestionService.syncLast48Hours();
        return new AdminDtos.SyncResponse(report.discovered(), report.stored());
    }

    @Transactional(readOnly = true)
    public List<AdminDtos.SourceResponse> listSources() {
        return sourceAdminService.listSources();
    }

    @Transactional
    public AdminDtos.SourceResponse createSource(AdminDtos.CreateSourceRequest request) {
        return sourceAdminService.createSource(request);
    }

    @Transactional
    public AdminDtos.SourceResponse updateSource(Long id, AdminDtos.UpdateSourceRequest request) {
        return sourceAdminService.updateSource(id, request);
    }

    @Transactional
    public void deleteSource(Long id, boolean deleteArticles) {
        sourceAdminService.deleteSource(id, deleteArticles);
    }

    @Transactional
    public AdminDtos.ClearArticlesResponse clearArticles() {
        ArticleStorageCleanupService.ClearArticlesResult result = articleStorageCleanupService.clearAllArticles();
        return new AdminDtos.ClearArticlesResponse(
                result.deletedRawArticles(),
                result.deletedPreparedArticles(),
                result.deletedPublications(),
                result.deletedMarkdownFiles()
        );
    }

    @Transactional
    public AdminDtos.TelegramValidationResponse validateTelegram(AdminDtos.TelegramConfigRequest request) {
        TelegramGateway.TelegramBotInfo info = telegramGateway.validate(request.botToken());
        TelegramConfig config = telegramConfigRepository.findFirstByActiveTrueOrderByUpdatedAtDesc().orElseGet(TelegramConfig::new);
        config.setActive(request.active());
        config.setBotToken(request.botToken());
        config.setChatId(request.chatId());
        config.setBotUsername(info.username());
        config.setValidated(true);
        config.setLastValidatedAt(OffsetDateTime.now(clock));
        if (config.getCreatedAt() == null) {
            config.setCreatedAt(OffsetDateTime.now(clock));
        }
        config.setUpdatedAt(OffsetDateTime.now(clock));
        telegramConfigRepository.save(config);
        return new AdminDtos.TelegramValidationResponse(info.botId(), info.username(), true);
    }

    @Transactional
    public void sendTelegramTestMessage(AdminDtos.TelegramConfigRequest request) {
        TelegramGateway.TelegramBotInfo info = telegramGateway.validate(request.botToken());
        telegramGateway.sendMessage(request.botToken(), request.chatId(),
                "Geek Parser test ping. Bot @" + info.username() + " готов к публикациям.");
    }

    @Transactional(readOnly = true)
    public AdminDtos.TelegramConfigResponse getTelegramConfig() {
        return telegramSettingsService.findCredentialsForAdmin()
                .map(credentials -> new AdminDtos.TelegramConfigResponse(
                        null,
                        true,
                        credentials.chatId(),
                        credentials.botUsername() == null ? "" : credentials.botUsername(),
                        credentials.validated(),
                        null,
                        credentials.fromEnv(),
                        telegramSettingsService.isDraftChatConfigured(),
                        telegramSettingsService.maskDraftChatId()
                ))
                .orElse(null);
    }

    private AdminDtos.ArticleSummaryResponse toSummary(PreparedArticle article) {
        return new AdminDtos.ArticleSummaryResponse(
                article.getId(),
                article.getRawArticle().getId(),
                article.getRawArticle().getSource().getCode(),
                article.getTitle(),
                article.getRawArticle().getCanonicalUrl(),
                article.getSummaryBody(),
                article.getStatus(),
                article.isNeedsManualReview(),
                article.getRawArticle().getPublishedAt(),
                article.getUpdatedAt()
        );
    }

    private AdminDtos.ArticleDetailsResponse toDetails(PreparedArticle article) {
        return new AdminDtos.ArticleDetailsResponse(
                article.getId(),
                article.getRawArticle().getId(),
                article.getRawArticle().getSource().getCode(),
                article.getTitle(),
                article.getRawArticle().getCanonicalUrl(),
                article.getRawArticle().getMarkdownPath(),
                article.getRawArticle().getRawExcerpt(),
                article.getTranslatedBody(),
                article.getSummaryBody(),
                article.getEditorialNotes(),
                article.getStatus(),
                article.isNeedsManualReview(),
                article.getRawArticle().getPublishedAt(),
                article.getUpdatedAt()
        );
    }

    private AdminDtos.PublicationResponse toPublication(PublicationJob job) {
        return new AdminDtos.PublicationResponse(
                job.getId(),
                job.getPreparedArticle().getId(),
                job.getPreparedArticle().getTitle(),
                job.getScheduledAt(),
                job.getStatus(),
                job.getAttemptCount(),
                job.getTelegramMessageId(),
                job.getLastError()
        );
    }
}
