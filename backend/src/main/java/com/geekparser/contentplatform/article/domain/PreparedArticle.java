package com.geekparser.contentplatform.article.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "prepared_articles")
public class PreparedArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "raw_article_id", nullable = false, unique = true)
    private RawArticle rawArticle;

    @Column(nullable = false, length = 512)
    private String title;

    @Column(columnDefinition = "text")
    private String translatedBody;

    @Column(columnDefinition = "text")
    private String summaryBody;

    @Column(columnDefinition = "text")
    private String editorialNotes;

    @Column(nullable = false)
    private boolean needsManualReview;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ArticleStatus status;

    @Column(length = 64)
    private String llmModel;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public RawArticle getRawArticle() {
        return rawArticle;
    }

    public void setRawArticle(RawArticle rawArticle) {
        this.rawArticle = rawArticle;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTranslatedBody() {
        return translatedBody;
    }

    public void setTranslatedBody(String translatedBody) {
        this.translatedBody = translatedBody;
    }

    public String getSummaryBody() {
        return summaryBody;
    }

    public void setSummaryBody(String summaryBody) {
        this.summaryBody = summaryBody;
    }

    public String getEditorialNotes() {
        return editorialNotes;
    }

    public void setEditorialNotes(String editorialNotes) {
        this.editorialNotes = editorialNotes;
    }

    public boolean isNeedsManualReview() {
        return needsManualReview;
    }

    public void setNeedsManualReview(boolean needsManualReview) {
        this.needsManualReview = needsManualReview;
    }

    public ArticleStatus getStatus() {
        return status;
    }

    public void setStatus(ArticleStatus status) {
        this.status = status;
    }

    public String getLlmModel() {
        return llmModel;
    }

    public void setLlmModel(String llmModel) {
        this.llmModel = llmModel;
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
