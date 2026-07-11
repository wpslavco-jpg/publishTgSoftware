package com.geekparser.contentplatform.ingestion.application;

import com.geekparser.contentplatform.ingestion.domain.Source;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class SourceScraper {

    private static final Pattern URL_PATTERN = Pattern.compile("https?://[\\w\\-./?=&%#]+");
    private static final List<String> EXCLUDED_PATH_FRAGMENTS = List.of(
            "/page/", "/category/", "/tag/", "/author/", "/feed/", "/wp-content/"
    );

    private final SourceProfileCatalog profileCatalog;
    private final WebClient.Builder webClientBuilder;

    public SourceScraper(SourceProfileCatalog profileCatalog, WebClient.Builder webClientBuilder) {
        this.profileCatalog = profileCatalog;
        this.webClientBuilder = webClientBuilder;
    }

    public List<FetchedArticle> fetchArticlesSince(Source source, OffsetDateTime cutoff) {
        SourceProfileCatalog.SourceProfile profile = profileCatalog.resolveProfile(source);

        Document listingDocument = fetchDocument(source.getListingUrl());
        Set<String> articleUrls = extractArticleUrls(listingDocument, profile);
        List<FetchedArticle> fetchedArticles = new ArrayList<>();
        for (String articleUrl : articleUrls) {
            try {
                FetchedArticle fetchedArticle = fetchArticle(articleUrl, profile);
                if (fetchedArticle.body().length() < 200) {
                    continue;
                }
                if (fetchedArticle.publishedAt().isBefore(cutoff)) {
                    continue;
                }
                fetchedArticles.add(fetchedArticle);
            } catch (Exception ignored) {
                // Individual source items may fail because publishers frequently tweak markup.
            }
        }
        return fetchedArticles;
    }

    private FetchedArticle fetchArticle(String articleUrl, SourceProfileCatalog.SourceProfile profile) {
        Document document = fetchDocument(articleUrl);
        String title = firstText(document, profile.titleSelectors(), document.title());
        String body = firstText(document, profile.bodySelectors(), "");
        OffsetDateTime publishedAt = extractPublishedAt(document, profile);
        return new FetchedArticle(articleUrl, title, normalizeWhitespace(body), publishedAt);
    }

    private Document fetchDocument(String url) {
        String body = webClientBuilder.build()
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        return Jsoup.parse(body, url);
    }

    private Set<String> extractArticleUrls(Document document, SourceProfileCatalog.SourceProfile profile) {
        Set<String> urls = new LinkedHashSet<>();
        for (Element link : document.select("a[href]")) {
            maybeAddUrl(urls, link.absUrl("href"), profile);
            if (urls.size() >= 25) {
                break;
            }
        }
        if (urls.size() < 10) {
            Matcher matcher = URL_PATTERN.matcher(document.text());
            while (matcher.find() && urls.size() < 25) {
                maybeAddUrl(urls, matcher.group(), profile);
            }
        }
        return urls;
    }

    private void maybeAddUrl(Set<String> urls, String href, SourceProfileCatalog.SourceProfile profile) {
        if (href == null || href.isBlank()) {
            return;
        }
        String normalized = stripFragment(href);
        if (EXCLUDED_PATH_FRAGMENTS.stream().anyMatch(normalized::contains)) {
            return;
        }
        boolean accepted = profile.articleUrlPatterns().stream().anyMatch(pattern -> pattern.matcher(normalized).matches());
        if (accepted) {
            urls.add(normalized);
        }
    }

    private String firstText(Document document, List<String> selectors, String fallback) {
        for (String selector : selectors) {
            Element element = document.selectFirst(selector);
            if (element != null) {
                String text = normalizeWhitespace(element.text());
                if (!text.isBlank()) {
                    return text;
                }
            }
        }
        return normalizeWhitespace(fallback);
    }

    private OffsetDateTime extractPublishedAt(Document document, SourceProfileCatalog.SourceProfile profile) {
        for (String selector : profile.publishedAtSelectors()) {
            Element element = document.selectFirst(selector);
            if (element == null) {
                continue;
            }
            String candidate = element.hasAttr("content") ? element.attr("content") : element.attr("datetime");
            if (candidate == null || candidate.isBlank()) {
                candidate = element.text();
            }
            OffsetDateTime parsed = tryParseDate(candidate);
            if (parsed != null) {
                return parsed;
            }
        }

        Element ldJson = document.selectFirst("script[type=application/ld+json]");
        if (ldJson != null) {
            String text = ldJson.data();
            int index = text.indexOf("\"datePublished\"");
            if (index > 0) {
                int startQuote = text.indexOf('"', index + 16);
                int endQuote = startQuote > 0 ? text.indexOf('"', startQuote + 1) : -1;
                if (startQuote > 0 && endQuote > startQuote) {
                    OffsetDateTime parsed = tryParseDate(text.substring(startQuote + 1, endQuote));
                    if (parsed != null) {
                        return parsed;
                    }
                }
            }
        }
        return OffsetDateTime.now().minusDays(1);
    }

    private OffsetDateTime tryParseDate(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return null;
        }
        String normalized = candidate.trim().replace(" г.", "").replace(" GMT", "Z");
        try {
            return OffsetDateTime.parse(normalized);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return ZonedDateTime.parse(normalized).toOffsetDateTime();
        } catch (DateTimeParseException ignored) {
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.contains("час") || lower.contains("day") || lower.contains("ago")) {
            return OffsetDateTime.now().minusHours(6);
        }
        return null;
    }

    private String normalizeWhitespace(String value) {
        return value == null ? "" : value.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
    }

    private String stripFragment(String href) {
        int hashIndex = href.indexOf('#');
        return hashIndex >= 0 ? href.substring(0, hashIndex) : href;
    }
}
