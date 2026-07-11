package com.geekparser.contentplatform.article.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RawArticleRepository extends JpaRepository<RawArticle, Long> {

    Optional<RawArticle> findByCanonicalUrl(String canonicalUrl);

    List<RawArticle> findByPublishedAtAfterOrderByPublishedAtDesc(OffsetDateTime cutoff);
}
