package com.geekparser.contentplatform.ingestion.application;

import com.geekparser.contentplatform.ingestion.domain.Source;
import com.geekparser.contentplatform.ingestion.domain.SourceType;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class SourceProfileCatalog {

    public static final String DEFAULT_BODY_SELECTORS = "article\n.article\n.entry-content\nmain";
    public static final String DEFAULT_TITLE_SELECTORS = "h1";
    public static final String DEFAULT_PUBLISHED_AT_SELECTORS =
            "meta[property=article:published_time]\ntime[datetime]\nmeta[name=date]";

    private final Map<String, SourceProfile> builtInProfiles = Map.of(
            "rozetked", profile(
                    "rozetked", "Rozetked", SourceType.HTML,
                    "https://rozetked.me", "https://rozetked.me/", null,
                    ".*/(news|articles|posts)/.*"
            ),
            "wylsa", profile(
                    "wylsa", "Wylsacom News", SourceType.HTML,
                    "https://wylsa.com", "https://wylsa.com/category/news/", null,
                    "https://wylsa\\.com/[a-z0-9][a-z0-9-]+/?$"
            ),
            "ign", profile(
                    "ign", "IGN News", SourceType.HTML,
                    "https://www.ign.com", "https://www.ign.com/news", null,
                    "https://www\\.ign\\.com/articles/.+"
            ),
            "macrumors", profile(
                    "macrumors", "MacRumors", SourceType.HTML,
                    "https://www.macrumors.com", "https://www.macrumors.com/", null,
                    "https://www\\.macrumors\\.com/\\d{4}/\\d{2}/\\d{2}/.+"
            ),
            "3dnews", profile(
                    "3dnews", "3DNews", SourceType.HTML,
                    "https://3dnews.ru", "https://3dnews.ru/", null,
                    "https://3dnews\\.ru/\\d+/.+"
            )
    );

    public List<SourceProfile> defaults() {
        return builtInProfiles.values().stream().toList();
    }

    public boolean isBuiltIn(String code) {
        return builtInProfiles.containsKey(code);
    }

    public Optional<SourceProfile> findBuiltInByCode(String code) {
        return Optional.ofNullable(builtInProfiles.get(code));
    }

    public SourceProfile resolveProfile(Source source) {
        if (source.hasScrapingProfile()) {
            return fromSource(source);
        }
        return findBuiltInByCode(source.getCode())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Source '%s' has no scraping profile. Configure URL patterns and selectors in admin."
                                .formatted(source.getCode())
                ));
    }

    public void applyProfileToSource(Source source, SourceProfile profile) {
        source.setCode(profile.code());
        source.setName(profile.name());
        source.setType(profile.type());
        source.setBaseUrl(profile.baseUrl());
        source.setListingUrl(profile.listingUrl());
        source.setRssUrl(profile.rssUrl());
        source.setArticleUrlPatterns(joinPatterns(profile.articleUrlPatterns()));
        source.setBodySelectors(joinLines(profile.bodySelectors()));
        source.setTitleSelectors(joinLines(profile.titleSelectors()));
        source.setPublishedAtSelectors(joinLines(profile.publishedAtSelectors()));
    }

    public void applyScrapingFieldsIfMissing(Source source, SourceProfile profile) {
        if (source.getArticleUrlPatterns() == null || source.getArticleUrlPatterns().isBlank()) {
            source.setArticleUrlPatterns(joinPatterns(profile.articleUrlPatterns()));
        }
        if (source.getBodySelectors() == null || source.getBodySelectors().isBlank()) {
            source.setBodySelectors(joinLines(profile.bodySelectors()));
        }
        if (source.getTitleSelectors() == null || source.getTitleSelectors().isBlank()) {
            source.setTitleSelectors(joinLines(profile.titleSelectors()));
        }
        if (source.getPublishedAtSelectors() == null || source.getPublishedAtSelectors().isBlank()) {
            source.setPublishedAtSelectors(joinLines(profile.publishedAtSelectors()));
        }
    }

    public static List<String> parseLines(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();
    }

    public static List<Pattern> parsePatterns(String value) {
        return parseLines(value).stream()
                .map(pattern -> {
                    try {
                        return Pattern.compile(pattern);
                    } catch (PatternSyntaxException exception) {
                        throw new IllegalArgumentException("Invalid regex pattern: " + pattern, exception);
                    }
                })
                .toList();
    }

    public static String joinLines(List<String> values) {
        return values.stream().collect(Collectors.joining("\n"));
    }

    private static String joinPatterns(List<Pattern> patterns) {
        return patterns.stream().map(Pattern::pattern).collect(Collectors.joining("\n"));
    }

    private static SourceProfile fromSource(Source source) {
        List<Pattern> patterns = parsePatterns(source.getArticleUrlPatterns());
        if (patterns.isEmpty()) {
            throw new IllegalArgumentException("Source '%s' requires at least one article URL pattern."
                    .formatted(source.getCode()));
        }
        List<String> bodySelectors = parseLinesOrDefault(source.getBodySelectors(), DEFAULT_BODY_SELECTORS);
        List<String> titleSelectors = parseLinesOrDefault(source.getTitleSelectors(), DEFAULT_TITLE_SELECTORS);
        List<String> publishedAtSelectors = parseLinesOrDefault(
                source.getPublishedAtSelectors(),
                DEFAULT_PUBLISHED_AT_SELECTORS
        );
        return new SourceProfile(
                source.getCode(),
                source.getName(),
                source.getType(),
                source.getBaseUrl(),
                source.getListingUrl(),
                source.getRssUrl(),
                patterns,
                bodySelectors,
                titleSelectors,
                publishedAtSelectors
        );
    }

    private static List<String> parseLinesOrDefault(String value, String defaultValue) {
        List<String> parsed = parseLines(value);
        return parsed.isEmpty() ? parseLines(defaultValue) : parsed;
    }

    private static SourceProfile profile(
            String code,
            String name,
            SourceType type,
            String baseUrl,
            String listingUrl,
            String rssUrl,
            String articlePattern
    ) {
        return new SourceProfile(
                code,
                name,
                type,
                baseUrl,
                listingUrl,
                rssUrl,
                List.of(Pattern.compile(articlePattern)),
                parseLines(DEFAULT_BODY_SELECTORS),
                parseLines(DEFAULT_TITLE_SELECTORS),
                parseLines(DEFAULT_PUBLISHED_AT_SELECTORS)
        );
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
