package com.geekparser.contentplatform.ingestion.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceRepository extends JpaRepository<Source, Long> {

    Optional<Source> findByCode(String code);

    boolean existsByCode(String code);

    List<Source> findByActiveTrueOrderByNameAsc();

    List<Source> findAllByOrderByNameAsc();
}
