package com.geekparser.contentplatform.ingestion.application;

import com.geekparser.contentplatform.ingestion.domain.Source;
import com.geekparser.contentplatform.ingestion.domain.SourceRepository;
import com.geekparser.contentplatform.ingestion.domain.SourceSeedExclusionRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SourceBootstrapService {

    private final SourceRepository sourceRepository;
    private final SourceProfileCatalog sourceProfileCatalog;
    private final SourceSeedExclusionRepository sourceSeedExclusionRepository;
    private final Clock clock;

    public SourceBootstrapService(SourceRepository sourceRepository,
                                  SourceProfileCatalog sourceProfileCatalog,
                                  SourceSeedExclusionRepository sourceSeedExclusionRepository,
                                  Clock clock) {
        this.sourceRepository = sourceRepository;
        this.sourceProfileCatalog = sourceProfileCatalog;
        this.sourceSeedExclusionRepository = sourceSeedExclusionRepository;
        this.clock = clock;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedSources() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        for (SourceProfileCatalog.SourceProfile profile : sourceProfileCatalog.defaults()) {
            Source source = sourceRepository.findByCode(profile.code()).orElseGet(Source::new);
            boolean isNew = source.getId() == null;
            if (isNew && sourceSeedExclusionRepository.existsById(profile.code())) {
                // Admin explicitly deleted this built-in source before; respect that and don't resurrect it.
                continue;
            }
            if (isNew) {
                sourceProfileCatalog.applyProfileToSource(source, profile);
                source.setActive(true);
                source.setCreatedAt(now);
            } else {
                sourceProfileCatalog.applyScrapingFieldsIfMissing(source, profile);
            }
            source.setUpdatedAt(now);
            sourceRepository.save(source);
        }
    }
}
