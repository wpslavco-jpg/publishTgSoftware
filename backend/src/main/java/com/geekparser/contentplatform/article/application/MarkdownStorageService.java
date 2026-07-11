package com.geekparser.contentplatform.article.application;

import com.geekparser.contentplatform.config.AppProperties;
import com.geekparser.contentplatform.ingestion.domain.Source;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

@Service
public class MarkdownStorageService {

    private final AppProperties appProperties;

    public MarkdownStorageService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public String saveRawArticle(Source source, String slug, String title, String canonicalUrl, OffsetDateTime publishedAt,
                                 String body) {
        Path directory = Path.of(appProperties.storage().rawDirectory())
                .resolve(String.valueOf(publishedAt.getYear()))
                .resolve(String.format("%02d", publishedAt.getMonthValue()))
                .resolve(String.format("%02d", publishedAt.getDayOfMonth()));
        Path filePath = directory.resolve(slug + ".md");
        String markdown = """
                ---
                source: %s
                title: %s
                canonicalUrl: %s
                publishedAt: %s
                ---

                %s
                """.formatted(
                source.getCode(),
                escapeYaml(title),
                canonicalUrl,
                publishedAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                body
        );
        try {
            Files.createDirectories(directory);
            Files.writeString(filePath, markdown);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to persist markdown article", exception);
        }
        return filePath.normalize().toString();
    }

    private String escapeYaml(String title) {
        return '"' + title.replace("\"", "\\\"") + '"';
    }
}
