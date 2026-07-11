package com.geekparser.contentplatform.publishing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.geekparser.contentplatform.config.AppProperties;
import com.geekparser.contentplatform.publishing.domain.TelegramConfig;
import com.geekparser.contentplatform.publishing.domain.TelegramConfigRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TelegramSettingsServiceTest {

    @Mock
    private TelegramConfigRepository telegramConfigRepository;

    @Test
    void shouldPreferEnvCredentialsOverDatabase() {
        AppProperties properties = new AppProperties(
                new AppProperties.Storage("../storage/articles/raw"),
                new AppProperties.Llm("https://ask.chadgpt.ru", "", "gpt-4o", Duration.ofSeconds(5), Duration.ofSeconds(30)),
                new AppProperties.Telegram("https://api.telegram.org", "", 0, "env-token", "-1001", "env-bot", ""),
                new AppProperties.TelegramUserClient("http://localhost:8090", true),
                new AppProperties.Scheduling("0 0 */2 * * *", "0/30 * * * * *"),
                new AppProperties.Cors("http://localhost:5173")
        );
        TelegramSettingsService service = new TelegramSettingsService(properties, telegramConfigRepository);

        Optional<TelegramSettingsService.TelegramCredentials> credentials = service.findActiveCredentials();

        assertThat(credentials).isPresent();
        assertThat(credentials.get().botToken()).isEqualTo("env-token");
        assertThat(credentials.get().chatId()).isEqualTo("-1001");
        assertThat(credentials.get().fromEnv()).isTrue();
    }

    @Test
    void shouldFallbackToDatabaseWhenEnvMissing() {
        AppProperties properties = new AppProperties(
                new AppProperties.Storage("../storage/articles/raw"),
                new AppProperties.Llm("https://ask.chadgpt.ru", "", "gpt-4o", Duration.ofSeconds(5), Duration.ofSeconds(30)),
                new AppProperties.Telegram("https://api.telegram.org", "", 0, "", "", "", ""),
                new AppProperties.TelegramUserClient("http://localhost:8090", true),
                new AppProperties.Scheduling("0 0 */2 * * *", "0/30 * * * * *"),
                new AppProperties.Cors("http://localhost:5173")
        );
        TelegramSettingsService service = new TelegramSettingsService(properties, telegramConfigRepository);

        TelegramConfig config = new TelegramConfig();
        config.setActive(true);
        config.setValidated(true);
        config.setBotToken("db-token");
        config.setChatId("-1002");
        config.setBotUsername("db-bot");
        config.setUpdatedAt(OffsetDateTime.parse("2026-07-11T00:00:00Z"));

        when(telegramConfigRepository.findFirstByActiveTrueOrderByUpdatedAtDesc())
                .thenReturn(Optional.of(config));

        Optional<TelegramSettingsService.TelegramCredentials> credentials = service.findActiveCredentials();

        assertThat(credentials).isPresent();
        assertThat(credentials.get().botToken()).isEqualTo("db-token");
        assertThat(credentials.get().chatId()).isEqualTo("-1002");
        assertThat(credentials.get().fromEnv()).isFalse();
    }

    @Test
    void shouldExposeEnvConfigForAdmin() {
        AppProperties properties = new AppProperties(
                new AppProperties.Storage("../storage/articles/raw"),
                new AppProperties.Llm("https://ask.chadgpt.ru", "", "gpt-4o", Duration.ofSeconds(5), Duration.ofSeconds(30)),
                new AppProperties.Telegram("https://api.telegram.org", "", 0, "env-token", "-1001", "env-bot", ""),
                new AppProperties.TelegramUserClient("http://localhost:8090", true),
                new AppProperties.Scheduling("0 0 */2 * * *", "0/30 * * * * *"),
                new AppProperties.Cors("http://localhost:5173")
        );
        TelegramSettingsService service = new TelegramSettingsService(properties, telegramConfigRepository);

        assertThat(service.isConfiguredViaEnv()).isTrue();
        assertThat(service.findCredentialsForAdmin()).isPresent();
    }
}
