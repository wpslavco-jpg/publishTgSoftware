package com.geekparser.contentplatform.ingestion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "sources")
public class Source {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SourceType type;

    @Column(nullable = false, length = 512)
    private String baseUrl;

    @Column(nullable = false, length = 512)
    private String listingUrl;

    @Column(length = 512)
    private String rssUrl;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @Column(columnDefinition = "text")
    private String articleUrlPatterns;

    @Column(columnDefinition = "text")
    private String bodySelectors;

    @Column(columnDefinition = "text")
    private String titleSelectors;

    @Column(columnDefinition = "text")
    private String publishedAtSelectors;

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SourceType getType() {
        return type;
    }

    public void setType(SourceType type) {
        this.type = type;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getListingUrl() {
        return listingUrl;
    }

    public void setListingUrl(String listingUrl) {
        this.listingUrl = listingUrl;
    }

    public String getRssUrl() {
        return rssUrl;
    }

    public void setRssUrl(String rssUrl) {
        this.rssUrl = rssUrl;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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

    public String getArticleUrlPatterns() {
        return articleUrlPatterns;
    }

    public void setArticleUrlPatterns(String articleUrlPatterns) {
        this.articleUrlPatterns = articleUrlPatterns;
    }

    public String getBodySelectors() {
        return bodySelectors;
    }

    public void setBodySelectors(String bodySelectors) {
        this.bodySelectors = bodySelectors;
    }

    public String getTitleSelectors() {
        return titleSelectors;
    }

    public void setTitleSelectors(String titleSelectors) {
        this.titleSelectors = titleSelectors;
    }

    public String getPublishedAtSelectors() {
        return publishedAtSelectors;
    }

    public void setPublishedAtSelectors(String publishedAtSelectors) {
        this.publishedAtSelectors = publishedAtSelectors;
    }

    public boolean hasScrapingProfile() {
        return articleUrlPatterns != null && !articleUrlPatterns.isBlank();
    }
}
