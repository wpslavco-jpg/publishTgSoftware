package com.geekparser.contentplatform.publishing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.geekparser.contentplatform.article.domain.PreparedArticle;
import com.geekparser.contentplatform.article.domain.PreparedArticleRepository;
import com.geekparser.contentplatform.article.domain.RawArticle;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TelegramDraftServiceTest {

    @Mock
    private PreparedArticleRepository preparedArticleRepository;

    @Mock
    private TelegramSettingsService telegramSettingsService;

    @Mock
    private TelegramGateway telegramGateway;

    @InjectMocks
    private TelegramDraftService telegramDraftService;

    @Test
    void shouldSendDraftToPersonalChat() {
        PreparedArticle article = mock(PreparedArticle.class);
        RawArticle rawArticle = mock(RawArticle.class);
        when(article.getId()).thenReturn(7L);
        when(article.getTitle()).thenReturn("Test title");
        when(article.getSummaryBody()).thenReturn("Summary with <b>bold</b>.");
        when(article.getRawArticle()).thenReturn(rawArticle);
        when(rawArticle.getCanonicalUrl()).thenReturn("https://example.com/article");

        when(preparedArticleRepository.findById(7L)).thenReturn(Optional.of(article));
        when(telegramSettingsService.requireActiveCredentials()).thenReturn(
                new TelegramSettingsService.TelegramCredentials("token", "-1001", "bot", true, true)
        );
        when(telegramSettingsService.requireDraftChatId()).thenReturn("123456789");
        when(telegramGateway.sendMessage(
                eq("token"),
                eq("123456789"),
                org.mockito.ArgumentMatchers.anyString(),
                eq("HTML"),
                eq(true)
        )).thenReturn("555");

        TelegramDraftService.DraftSendResult result = telegramDraftService.sendDraft(7L);

        assertThat(result.messageId()).isEqualTo("555");
        assertThat(result.draftChatId()).isEqualTo("123456789");
        verify(telegramGateway).sendMessage(
                eq("token"),
                eq("123456789"),
                org.mockito.ArgumentMatchers.contains("Черновик #7"),
                eq("HTML"),
                eq(true)
        );
    }
}
