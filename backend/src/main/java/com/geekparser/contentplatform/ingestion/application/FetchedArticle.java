package com.geekparser.contentplatform.ingestion.application;

import java.time.OffsetDateTime;

public record FetchedArticle(
        String canonicalUrl,
        String title,
        String body,
        OffsetDateTime publishedAt
) {
}
