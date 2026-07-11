package com.geekparser.contentplatform.publishing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.geekparser.contentplatform.article.domain.ArticleStatus;
import com.geekparser.contentplatform.article.domain.PreparedArticle;
import com.geekparser.contentplatform.article.domain.PreparedArticleRepository;
import com.geekparser.contentplatform.publishing.domain.PublicationJob;
import com.geekparser.contentplatform.publishing.domain.PublicationJobRepository;
import com.geekparser.contentplatform.publishing.domain.PublicationStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublishingServiceTest {

    @Mock
    private PublicationJobRepository publicationJobRepository;
    @Mock
    private PreparedArticleRepository preparedArticleRepository;
    @Mock
    private TelegramSettingsService telegramSettingsService;
    @Mock
    private TelegramGateway telegramGateway;
    @Mock
    private TelegramUserClientGateway telegramUserClientGateway;

    private PublishingService publishingService;
    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-07-08T12:00:00Z"), ZoneOffset.UTC);
        publishingService = new PublishingService(
                publicationJobRepository,
                preparedArticleRepository,
                telegramSettingsService,
                telegramGateway,
                telegramUserClientGateway,
                clock
        );
    }

    @Test
    void shouldPublishDueJobs() {
        PreparedArticle article = new PreparedArticle();
        article.setTitle("Test");
        article.setSummaryBody("Summary text");
        article.setStatus(ArticleStatus.SCHEDULED);

        PublicationJob job = new PublicationJob();
        job.setPreparedArticle(article);
        job.setStatus(PublicationStatus.SCHEDULED);
        job.setScheduledAt(OffsetDateTime.now(clock).minusMinutes(5));
        job.setAttemptCount(0);

        when(telegramSettingsService.findActiveCredentials()).thenReturn(Optional.of(credentials()));
        when(publicationJobRepository.findDueJobs(PublicationStatus.SCHEDULED, OffsetDateTime.now(clock)))
                .thenReturn(List.of(job));
        when(telegramGateway.sendMessage(anyString(), anyString(), anyString(), isNull())).thenReturn("42");

        publishingService.processDueJobs();

        assertThat(job.getStatus()).isEqualTo(PublicationStatus.PUBLISHED);
        assertThat(job.getTelegramMessageId()).isEqualTo("42");
        assertThat(article.getStatus()).isEqualTo(ArticleStatus.PUBLISHED);
    }

    @Test
    void shouldRegisterNativeTelegramScheduleWhenUserClientEnabled() {
        PreparedArticle article = new PreparedArticle();
        article.setSummaryBody("Summary text");

        when(telegramSettingsService.requireActiveCredentials()).thenReturn(credentials());
        when(telegramUserClientGateway.isEnabled()).thenReturn(true);
        when(telegramUserClientGateway.isAvailable()).thenReturn(true);
        when(telegramUserClientGateway.schedule(eq("chat"), eq("Summary text"), any(OffsetDateTime.class)))
                .thenReturn(new TelegramUserClientGateway.ScheduleResponse(
                        777,
                        OffsetDateTime.now(clock).plusHours(2),
                        "chat"
                ));
        when(preparedArticleRepository.findById(1L)).thenReturn(Optional.of(article));
        when(preparedArticleRepository.save(article)).thenReturn(article);
        when(publicationJobRepository.save(org.mockito.ArgumentMatchers.any(PublicationJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OffsetDateTime future = OffsetDateTime.now(clock).plusHours(2);
        PublicationJob job = publishingService.schedule(1L, future);

        assertThat(job.getStatus()).isEqualTo(PublicationStatus.REGISTERED_IN_TELEGRAM);
        assertThat(job.getTelegramMessageId()).isEqualTo("777");
        verify(telegramGateway, never()).sendMessage(anyString(), anyString(), anyString(), isNull());
    }

    @Test
    void shouldRejectScheduleTooSoon() {
        when(telegramSettingsService.requireActiveCredentials()).thenReturn(credentials());

        assertThatThrownBy(() -> publishingService.schedule(1L, OffsetDateTime.now(clock).plusSeconds(30)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least");
    }

    @Test
    void shouldPublishImmediately() {
        PreparedArticle article = new PreparedArticle();
        article.setSummaryBody("Summary text");

        when(telegramSettingsService.requireActiveCredentials()).thenReturn(credentials());
        when(preparedArticleRepository.findById(1L)).thenReturn(Optional.of(article));
        when(preparedArticleRepository.save(article)).thenReturn(article);
        when(telegramGateway.sendMessage(eq("token"), eq("chat"), anyString(), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn("99");
        when(publicationJobRepository.save(org.mockito.ArgumentMatchers.any(PublicationJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PublicationJob job = publishingService.publishNow(1L);

        assertThat(job.getStatus()).isEqualTo(PublicationStatus.PUBLISHED);
        assertThat(job.getTelegramMessageId()).isEqualTo("99");
        assertThat(article.getStatus()).isEqualTo(ArticleStatus.PUBLISHED);

        ArgumentCaptor<PublicationJob> captor = ArgumentCaptor.forClass(PublicationJob.class);
        verify(publicationJobRepository).save(captor.capture());
        assertThat(captor.getValue().getAttemptCount()).isEqualTo(1);
    }

    private TelegramSettingsService.TelegramCredentials credentials() {
        return new TelegramSettingsService.TelegramCredentials("token", "chat", "bot", true, true);
    }
}
