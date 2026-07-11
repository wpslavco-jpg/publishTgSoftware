package com.geekparser.contentplatform.article.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.geekparser.contentplatform.config.AppProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class LlmRewriteServiceTest {

    @Test
    void shouldFailWhenApiKeyMissing() {
        AppProperties properties = new AppProperties(
                new AppProperties.Storage("../storage/articles/raw"),
                new AppProperties.Llm("https://ask.chadgpt.ru", "", "gpt-4o", Duration.ofSeconds(5), Duration.ofSeconds(30)),
                new AppProperties.Telegram("https://api.telegram.org", "", 0, "", "", "", ""),
                new AppProperties.TelegramUserClient("http://localhost:8090", true),
                new AppProperties.Scheduling("0 0 */2 * * *", "0 */1 * * * *"),
                new AppProperties.Cors("http://localhost:5173")
        );
        LlmRewriteService service = new LlmRewriteService(properties, WebClient.builder());

        assertThatThrownBy(() -> service.rewrite("Sample title", "First sentence. Second sentence."))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LLM API key is required");
    }
}
