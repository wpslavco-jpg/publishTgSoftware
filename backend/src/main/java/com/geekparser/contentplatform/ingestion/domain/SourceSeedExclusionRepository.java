package com.geekparser.contentplatform.ingestion.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceSeedExclusionRepository extends JpaRepository<SourceSeedExclusion, String> {
}
