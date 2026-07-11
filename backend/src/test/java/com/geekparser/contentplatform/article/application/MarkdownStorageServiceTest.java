package com.geekparser.contentplatform.article.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.geekparser.contentplatform.config.AppProperties;
import com.geekparser.contentplatform.ingestion.domain.Source;
import com.geekparser.contentplatform.ingestion.domain.SourceType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MarkdownStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldPersistMarkdownWithFrontMatter() throws Exception {
        AppProperties properties = new AppProperties(
                new AppProperties.Storage(tempDir.toString()),
                new AppProperties.Llm("https://ask.chadgpt.ru", "", "gpt-4o", null, null),
                new AppProperties.Telegram("https://api.telegram.org", "", 0, "", "", "", ""),
                new AppProperties.TelegramUserClient("http://localhost:8090", true),
                new AppProperties.Scheduling("0 0 */2 * * *", "0 */1 * * * *"),
                new AppProperties.Cors("http://localhost:5173")
        );
        MarkdownStorageService service = new MarkdownStorageService(properties);

        Source source = new Source();
        source.setCode("macrumors");
        source.setType(SourceType.HTML);

        String path = service.saveRawArticle(
                source,
                "sample-article",
                "Sample Article",
                "https://example.com/article",
                OffsetDateTime.parse("2026-07-08T10:00:00Z"),
                "Article body"
        );

        Path saved = Path.of(path);
        assertThat(Files.exists(saved)).isTrue();
        assertThat(Files.readString(saved)).contains("source: macrumors").contains("Article body");
    }
}
