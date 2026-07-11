package com.geekparser.contentplatform.article.domain;

public enum ArticleStatus {
    DISCOVERED,
    RAW_SAVED,
    AI_PROCESSING,
    READY_FOR_REVIEW,
    READY_TO_PUBLISH,
    SCHEDULED,
    PUBLISHED,
    FAILED
}
