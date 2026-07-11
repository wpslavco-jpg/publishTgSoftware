package com.geekparser.contentplatform.ingestion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "source_seed_exclusions")
public class SourceSeedExclusion {

    @Id
    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false)
    private OffsetDateTime excludedAt;

    protected SourceSeedExclusion() {
    }

    public SourceSeedExclusion(String code, OffsetDateTime excludedAt) {
        this.code = code;
        this.excludedAt = excludedAt;
    }

    public String getCode() {
        return code;
    }

    public OffsetDateTime getExcludedAt() {
        return excludedAt;
    }
}
