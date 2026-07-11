package com.geekparser.contentplatform.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Storage storage,
        Llm llm,
        Telegram telegram,
        TelegramUserClient telegramUserClient,
        Scheduling scheduling,
        Cors cors
) {

    public record Storage(String rawDirectory) {
    }

    public record Llm(String baseUrl, String apiKey, String model, Duration connectTimeout, Duration readTimeout) {
    }

    public record Telegram(
            String apiBaseUrl,
            String proxyHost,
            int proxyPort,
            String botToken,
            String chatId,
            String botUsername,
            String draftChatId
    ) {
    }

    public record TelegramUserClient(String baseUrl, boolean enabled) {
    }

    public record Scheduling(String ingestionCron, String publicationCron) {
    }

    public record Cors(String allowedOrigin) {
    }
}
