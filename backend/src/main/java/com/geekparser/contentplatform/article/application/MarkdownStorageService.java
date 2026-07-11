package com.geekparser.contentplatform.article.application;

import com.geekparser.contentplatform.config.AppProperties;
import com.geekparser.contentplatform.ingestion.domain.Source;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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

    public int deleteFiles(List<String> markdownPaths) {
        int deleted = 0;
        for (String markdownPath : markdownPaths) {
            if (markdownPath == null || markdownPath.isBlank()) {
                continue;
            }
            try {
                if (Files.deleteIfExists(Path.of(markdownPath))) {
                    deleted++;
                }
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to delete markdown file: " + markdownPath, exception);
            }
        }
        return deleted;
    }

    public int clearRawStorageDirectory() {
        Path root = Path.of(appProperties.storage().rawDirectory());
        if (!Files.exists(root)) {
            return 0;
        }
        List<Path> filesToDelete = new ArrayList<>();
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.getFileName().toString().endsWith(".md")) {
                        filesToDelete.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to scan raw storage directory", exception);
        }
        int deleted = 0;
        for (Path file : filesToDelete) {
            try {
                Files.deleteIfExists(file);
                deleted++;
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to delete markdown file: " + file, exception);
            }
        }
        return deleted;
    }

    private String escapeYaml(String title) {
        return '"' + title.replace("\"", "\\\"") + '"';
    }
}
