package com.geekparser.contentplatform.publishing.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TelegramMessageFormatterTest {

    @Test
    void shouldFormatPlainPostWithTitle() {
        assertThat(TelegramMessageFormatter.formatPost("Заголовок", "Текст поста"))
                .isEqualTo("Заголовок\n\nТекст поста");
        assertThat(TelegramMessageFormatter.resolveParseMode("Текст поста")).isNull();
    }

    @Test
    void shouldFormatHtmlPostWithBoldTitle() {
        String body = "Обычный текст с <b>акцентом</b>.";
        assertThat(TelegramMessageFormatter.formatPost("News", body))
                .isEqualTo("<b>News</b>\n\n" + body);
        assertThat(TelegramMessageFormatter.resolveParseMode(body)).isEqualTo("HTML");
    }

    @Test
    void shouldFormatDraftMessageWithSourceLink() {
        String draft = TelegramMessageFormatter.formatDraftMessage(
                42L,
                "Заголовок",
                "Текст поста.",
                "https://example.com/news"
        );
        assertThat(draft)
                .contains("Черновик #42")
                .contains("<b>Заголовок</b>")
                .contains("Текст поста.")
                .contains("href=\"https://example.com/news\"");
        assertThat(TelegramMessageFormatter.draftParseMode()).isEqualTo("HTML");
    }
}
