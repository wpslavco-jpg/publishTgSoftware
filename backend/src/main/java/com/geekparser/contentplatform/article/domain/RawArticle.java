package com.geekparser.contentplatform.article.domain;

import com.geekparser.contentplatform.ingestion.domain.Source;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "raw_articles")
public class RawArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_id", nullable = false)
    private Source source;

    @Column(nullable = false, unique = true, length = 1024)
    private String canonicalUrl;

    @Column(nullable = false, length = 512)
    private String title;

    @Column(nullable = false, length = 256)
    private String slug;

    @Column(nullable = false, length = 128)
    private String contentHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ArticleStatus status;

    @Column(nullable = false)
    private OffsetDateTime publishedAt;

    @Column(nullable = false, length = 1024)
    private String markdownPath;

    @Column(columnDefinition = "text")
    private String rawExcerpt;

    @Column(columnDefinition = "text")
    private String sourcePayload;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public String getCanonicalUrl() {
        return canonicalUrl;
    }

    public void setCanonicalUrl(String canonicalUrl) {
        this.canonicalUrl = canonicalUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public ArticleStatus getStatus() {
        return status;
    }

    public void setStatus(ArticleStatus status) {
        this.status = status;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(OffsetDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public String getMarkdownPath() {
        return markdownPath;
    }

    public void setMarkdownPath(String markdownPath) {
        this.markdownPath = markdownPath;
    }

    public String getRawExcerpt() {
        return rawExcerpt;
    }

    public void setRawExcerpt(String rawExcerpt) {
        this.rawExcerpt = rawExcerpt;
    }

    public String getSourcePayload() {
        return sourcePayload;
    }

    public void setSourcePayload(String sourcePayload) {
        this.sourcePayload = sourcePayload;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
