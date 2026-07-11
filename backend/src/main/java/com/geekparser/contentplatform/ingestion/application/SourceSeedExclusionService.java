package com.geekparser.contentplatform.ingestion.application;

import com.geekparser.contentplatform.ingestion.domain.SourceSeedExclusion;
import com.geekparser.contentplatform.ingestion.domain.SourceSeedExclusionRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SourceSeedExclusionService {

    private final SourceSeedExclusionRepository sourceSeedExclusionRepository;
    private final SourceProfileCatalog sourceProfileCatalog;
    private final Clock clock;

    public SourceSeedExclusionService(SourceSeedExclusionRepository sourceSeedExclusionRepository,
                                      SourceProfileCatalog sourceProfileCatalog,
                                      Clock clock) {
        this.sourceSeedExclusionRepository = sourceSeedExclusionRepository;
        this.sourceProfileCatalog = sourceProfileCatalog;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public boolean isExcluded(String code) {
        return sourceSeedExclusionRepository.existsById(code);
    }

    @Transactional
    public void excludeBuiltInIfNeeded(String code) {
        if (sourceProfileCatalog.isBuiltIn(code)) {
            sourceSeedExclusionRepository.save(new SourceSeedExclusion(code, OffsetDateTime.now(clock)));
        }
    }

    @Transactional
    public void allowReseed(String code) {
        sourceSeedExclusionRepository.deleteById(code);
    }
}
