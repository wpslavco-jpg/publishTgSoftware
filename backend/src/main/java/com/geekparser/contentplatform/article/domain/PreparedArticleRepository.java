package com.geekparser.contentplatform.article.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PreparedArticleRepository extends JpaRepository<PreparedArticle, Long> {

    Optional<PreparedArticle> findByRawArticleId(Long rawArticleId);

    List<PreparedArticle> findAllByOrderByUpdatedAtDesc();

    @Query("select p.id from PreparedArticle p where p.rawArticle.source.id = :sourceId")
    List<Long> findIdsBySourceId(@Param("sourceId") Long sourceId);
}
