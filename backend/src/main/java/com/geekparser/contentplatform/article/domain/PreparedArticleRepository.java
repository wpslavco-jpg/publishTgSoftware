package com.geekparser.contentplatform.article.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreparedArticleRepository extends JpaRepository<PreparedArticle, Long> {

    Optional<PreparedArticle> findByRawArticleId(Long rawArticleId);

    List<PreparedArticle> findAllByOrderByUpdatedAtDesc();
}
