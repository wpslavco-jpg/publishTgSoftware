package com.geekparser.contentplatform.ingestion.application;

import com.geekparser.contentplatform.ingestion.domain.SourceType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SourceProfileCatalog {

    private final Map<String, SourceProfile> profiles = Map.of(
            "rozetked", new SourceProfile(
                    "rozetked",
                    "Rozetked",
                    SourceType.HTML,
                    "https://rozetked.me",
                    "https://rozetked.me/",
                    null,
                    List.of(Pattern.compile(".*/(news|articles|posts)/.*")),
                    List.of("article", ".article", ".entry-content", "main"),
                    List.of("h1"),
                    List.of("meta[property=article:published_time]", "time[datetime]", "meta[name=date]")
            ),
            "wylsa", new SourceProfile(
                    "wylsa",
                    "Wylsacom News",
                    SourceType.HTML,
                    "https://wylsa.com",
                    "https://wylsa.com/category/news/",
                    null,
                    List.of(Pattern.compile("https://wylsa\\.com/[a-z0-9][a-z0-9-]+/?$", Pattern.CASE_INSENSITIVE)),
                    List.of("article", ".entry-content", ".td-post-content", "main"),
                    List.of("h1"),
                    List.of("meta[property=article:published_time]", "time[datetime]", "meta[name=date]")
            ),
            "ign", new SourceProfile(
                    "ign",
                    "IGN News",
                    SourceType.HTML,
                    "https://www.ign.com",
                    "https://www.ign.com/news",
                    null,
                    List.of(Pattern.compile("https://www\\.ign\\.com/articles/.+")),
                    List.of("article", "[data-cy=article-page]", ".article-content", "main"),
                    List.of("h1"),
                    List.of("meta[property=article:published_time]", "meta[name=publish-date]", "time[datetime]")
            ),
            "macrumors", new SourceProfile(
                    "macrumors",
                    "MacRumors",
                    SourceType.HTML,
                    "https://www.macrumors.com",
                    "https://www.macrumors.com/",
                    null,
                    List.of(Pattern.compile("https://www\\.macrumors\\.com/\\d{4}/\\d{2}/\\d{2}/.+")),
                    List.of("article", ".article-content", ".content", "main"),
                    List.of("h1"),
                    List.of("meta[property=article:published_time]", "time[datetime]")
            ),
            "3dnews", new SourceProfile(
                    "3dnews",
                    "3DNews",
                    SourceType.HTML,
                    "https://3dnews.ru",
                    "https://3dnews.ru/",
                    null,
                    List.of(Pattern.compile("https://3dnews\\.ru/\\d+/.+")),
                    List.of("article", ".article-entry", ".js-mediator-article", "main"),
                    List.of("h1"),
                    List.of("meta[property=article:published_time]", "time[datetime]", "meta[name=date]")
            )
    );

    public List<SourceProfile> defaults() {
        return profiles.values().stream().toList();
    }

    public Optional<SourceProfile> findByCode(String code) {
        return Optional.ofNullable(profiles.get(code));
    }

    public record SourceProfile(
            String code,
            String name,
            SourceType type,
            String baseUrl,
            String listingUrl,
            String rssUrl,
            List<Pattern> articleUrlPatterns,
            List<String> bodySelectors,
            List<String> titleSelectors,
            List<String> publishedAtSelectors
    ) {
    }
}
