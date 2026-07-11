package com.geekparser.contentplatform.admin.api;

import com.geekparser.contentplatform.article.domain.ArticleStatus;
import com.geekparser.contentplatform.ingestion.domain.SourceType;
import com.geekparser.contentplatform.publishing.domain.PublicationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record ArticleSummaryResponse(
            Long preparedArticleId,
            Long rawArticleId,
            String sourceCode,
            String title,
            String canonicalUrl,
            String summaryBody,
            ArticleStatus status,
            boolean needsManualReview,
            OffsetDateTime publishedAt,
            OffsetDateTime updatedAt
    ) {
    }

    public record ArticleDetailsResponse(
            Long preparedArticleId,
            Long rawArticleId,
            String sourceCode,
            String title,
            String canonicalUrl,
            String markdownPath,
            String rawExcerpt,
            String translatedBody,
            String summaryBody,
            String editorialNotes,
            ArticleStatus status,
            boolean needsManualReview,
            OffsetDateTime publishedAt,
            OffsetDateTime updatedAt
    ) {
    }

    public record UpdateArticleRequest(
            @NotBlank String title,
            @NotBlank String summaryBody,
            String translatedBody,
            String editorialNotes,
            boolean readyToPublish
    ) {
    }

    public record SchedulePublicationRequest(
            @NotNull OffsetDateTime scheduledAt
    ) {
    }

    public record PublicationResponse(
            Long id,
            Long preparedArticleId,
            String title,
            OffsetDateTime scheduledAt,
            PublicationStatus status,
            int attemptCount,
            String telegramMessageId,
            String lastError
    ) {
    }

    public record TelegramConfigRequest(
            @NotBlank String botToken,
            @NotBlank String chatId,
            boolean active
    ) {
    }

    public record TelegramConfigResponse(
            Long id,
            boolean active,
            String chatId,
            String botUsername,
            boolean validated,
            OffsetDateTime lastValidatedAt,
            boolean configuredViaEnv,
            boolean draftChatConfigured,
            String draftChatIdMasked
    ) {
    }

    public record TelegramValidationResponse(
            String botId,
            String botUsername,
            boolean validated
    ) {
    }

    public record SyncResponse(int discovered, int stored) {
    }

    public record DiscoverChatsRequest(String botToken) {
    }

    public record SendDraftResponse(String messageId, String draftChatId) {
    }

    public record DiscoveredChatResponse(
            String chatId,
            String username,
            String firstName,
            String lastName,
            String lastMessageText
    ) {
    }

    public record SourceResponse(
            Long id,
            String code,
            String name,
            SourceType type,
            String baseUrl,
            String listingUrl,
            String rssUrl,
            boolean active,
            boolean builtIn,
            String articleUrlPatterns,
            String bodySelectors,
            String titleSelectors,
            String publishedAtSelectors,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }

    public record CreateSourceRequest(
            @NotBlank String code,
            @NotBlank String name,
            @NotNull SourceType type,
            @NotBlank String baseUrl,
            @NotBlank String listingUrl,
            String rssUrl,
            boolean active,
            @NotBlank String articleUrlPatterns,
            String bodySelectors,
            String titleSelectors,
            String publishedAtSelectors
    ) {
    }

    public record UpdateSourceRequest(
            @NotBlank String name,
            @NotNull SourceType type,
            @NotBlank String baseUrl,
            @NotBlank String listingUrl,
            String rssUrl,
            boolean active,
            @NotBlank String articleUrlPatterns,
            String bodySelectors,
            String titleSelectors,
            String publishedAtSelectors
    ) {
    }

    public record ClearArticlesResponse(
            long deletedRawArticles,
            long deletedPreparedArticles,
            long deletedPublications,
            int deletedMarkdownFiles
    ) {
    }
}
