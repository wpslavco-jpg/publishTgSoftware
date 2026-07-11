package com.geekparser.contentplatform.publishing.application;

import com.geekparser.contentplatform.article.domain.ArticleStatus;
import com.geekparser.contentplatform.article.domain.PreparedArticle;
import com.geekparser.contentplatform.article.domain.PreparedArticleRepository;
import com.geekparser.contentplatform.publishing.domain.PublicationJob;
import com.geekparser.contentplatform.publishing.domain.PublicationJobRepository;
import com.geekparser.contentplatform.publishing.domain.PublicationStatus;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublishingService {

    private static final int MIN_SCHEDULE_LEAD_SECONDS = 60;

    private final PublicationJobRepository publicationJobRepository;
    private final PreparedArticleRepository preparedArticleRepository;
    private final TelegramSettingsService telegramSettingsService;
    private final TelegramGateway telegramGateway;
    private final TelegramUserClientGateway telegramUserClientGateway;
    private final Clock clock;

    public PublishingService(PublicationJobRepository publicationJobRepository,
                             PreparedArticleRepository preparedArticleRepository,
                             TelegramSettingsService telegramSettingsService,
                             TelegramGateway telegramGateway,
                             TelegramUserClientGateway telegramUserClientGateway,
                             Clock clock) {
        this.publicationJobRepository = publicationJobRepository;
        this.preparedArticleRepository = preparedArticleRepository;
        this.telegramSettingsService = telegramSettingsService;
        this.telegramGateway = telegramGateway;
        this.telegramUserClientGateway = telegramUserClientGateway;
        this.clock = clock;
    }

    @Transactional
    public PublicationJob schedule(Long preparedArticleId, OffsetDateTime scheduledAt) {
        TelegramSettingsService.TelegramCredentials credentials = telegramSettingsService.requireActiveCredentials();
        OffsetDateTime now = OffsetDateTime.now(clock);
        if (!scheduledAt.isAfter(now.plusSeconds(MIN_SCHEDULE_LEAD_SECONDS))) {
            throw new IllegalArgumentException(
                    "Scheduled time must be at least %d seconds in the future. Use publish-now for immediate delivery."
                            .formatted(MIN_SCHEDULE_LEAD_SECONDS)
            );
        }

        PreparedArticle article = preparedArticleRepository.findById(preparedArticleId)
                .orElseThrow(() -> new IllegalArgumentException("Prepared article not found"));

        PublicationJob job = new PublicationJob();
        job.setPreparedArticle(article);
        job.setScheduledAt(scheduledAt);
        job.setAttemptCount(0);
        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        article.setStatus(ArticleStatus.SCHEDULED);
        article.setUpdatedAt(now);

        if (telegramUserClientGateway.isEnabled()) {
            if (!telegramUserClientGateway.isAvailable()) {
                throw new IllegalStateException(
                        "Telegram user client is not ready. Run ./scripts/setup-telegram-user.sh and start telegram-user-client."
                );
            }
            TelegramUserClientGateway.ScheduleResponse response = telegramUserClientGateway.schedule(
                    credentials.chatId(),
                    formatTelegramText(article),
                    scheduledAt
            );
            job.setStatus(PublicationStatus.REGISTERED_IN_TELEGRAM);
            job.setTelegramMessageId(String.valueOf(response.scheduledMessageId()));
            job.setLastError(null);
        } else {
            job.setStatus(PublicationStatus.SCHEDULED);
        }

        preparedArticleRepository.save(article);
        return publicationJobRepository.save(job);
    }

    @Transactional
    public PublicationJob publishNow(Long preparedArticleId) {
        TelegramSettingsService.TelegramCredentials credentials = telegramSettingsService.requireActiveCredentials();
        PreparedArticle article = preparedArticleRepository.findById(preparedArticleId)
                .orElseThrow(() -> new IllegalArgumentException("Prepared article not found"));
        OffsetDateTime now = OffsetDateTime.now(clock);

        PublicationJob job = new PublicationJob();
        job.setPreparedArticle(article);
        job.setScheduledAt(now);
        job.setAttemptCount(1);
        job.setCreatedAt(now);
        job.setUpdatedAt(now);

        try {
            String messageId = sendFormattedMessage(
                    credentials,
                    article
            );
            job.setStatus(PublicationStatus.PUBLISHED);
            job.setTelegramMessageId(messageId);
            article.setStatus(ArticleStatus.PUBLISHED);
        } catch (Exception exception) {
            job.setStatus(PublicationStatus.FAILED);
            job.setLastError(exception.getMessage());
            article.setStatus(ArticleStatus.FAILED);
        }

        job.setUpdatedAt(OffsetDateTime.now(clock));
        article.setUpdatedAt(OffsetDateTime.now(clock));
        preparedArticleRepository.save(article);
        return publicationJobRepository.save(job);
    }

    @Transactional
    public void processDueJobs() {
        TelegramSettingsService.TelegramCredentials credentials = telegramSettingsService.findActiveCredentials()
                .orElse(null);
        if (credentials == null) {
            return;
        }

        List<PublicationJob> jobs = publicationJobRepository.findDueJobs(
                PublicationStatus.SCHEDULED,
                OffsetDateTime.now(clock)
        );
        for (PublicationJob job : jobs) {
            publishJob(job, credentials);
        }
    }

    private void publishJob(PublicationJob job, TelegramSettingsService.TelegramCredentials credentials) {
        try {
            job.setStatus(PublicationStatus.PUBLISHING);
            job.setAttemptCount(job.getAttemptCount() + 1);
            job.setUpdatedAt(OffsetDateTime.now(clock));
            String messageId = sendFormattedMessage(credentials, job.getPreparedArticle());
            job.setStatus(PublicationStatus.PUBLISHED);
            job.setTelegramMessageId(messageId);
            job.setLastError(null);
            job.setUpdatedAt(OffsetDateTime.now(clock));
            PreparedArticle article = job.getPreparedArticle();
            article.setStatus(ArticleStatus.PUBLISHED);
            article.setUpdatedAt(OffsetDateTime.now(clock));
        } catch (Exception exception) {
            job.setStatus(PublicationStatus.FAILED);
            job.setLastError(exception.getMessage());
            job.setUpdatedAt(OffsetDateTime.now(clock));
            PreparedArticle article = job.getPreparedArticle();
            article.setStatus(ArticleStatus.FAILED);
            article.setUpdatedAt(OffsetDateTime.now(clock));
        }
    }

    private String sendFormattedMessage(TelegramSettingsService.TelegramCredentials credentials,
                                        PreparedArticle article) {
        String text = formatTelegramText(article);
        String parseMode = TelegramMessageFormatter.resolveParseMode(article.getSummaryBody());
        return telegramGateway.sendMessage(
                credentials.botToken(),
                credentials.chatId(),
                text,
                parseMode
        );
    }

    private static String formatTelegramText(PreparedArticle article) {
        return TelegramMessageFormatter.formatPost(article.getTitle(), article.getSummaryBody());
    }
}
