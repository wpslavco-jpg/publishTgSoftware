package com.geekparser.contentplatform.article.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RawArticleRepository extends JpaRepository<RawArticle, Long> {

    Optional<RawArticle> findByCanonicalUrl(String canonicalUrl);

    List<RawArticle> findByPublishedAtAfterOrderByPublishedAtDesc(java.time.OffsetDateTime cutoff);

    List<RawArticle> findBySourceId(Long sourceId);

    @Query("select r.markdownPath from RawArticle r")
    List<String> findAllMarkdownPaths();
}
