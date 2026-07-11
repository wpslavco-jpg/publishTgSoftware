package com.geekparser.contentplatform.ingestion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.geekparser.contentplatform.ingestion.domain.Source;
import com.geekparser.contentplatform.ingestion.domain.SourceRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SourceBootstrapServiceTest {

    private static final List<String> BUILT_IN_CODES = List.of("rozetked", "wylsa", "ign", "macrumors", "3dnews");

    @Mock
    private SourceRepository sourceRepository;

    @Mock
    private SourceSeedExclusionService sourceSeedExclusionService;

    private SourceBootstrapService sourceBootstrapService;

    @BeforeEach
    void setUp() {
        sourceBootstrapService = new SourceBootstrapService(
                sourceRepository,
                new SourceProfileCatalog(),
                sourceSeedExclusionService,
                Clock.fixed(Instant.parse("2026-07-11T12:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void shouldNotReseedExcludedBuiltInSource() {
        for (String code : BUILT_IN_CODES) {
            when(sourceRepository.findByCode(code)).thenReturn(Optional.empty());
            when(sourceSeedExclusionService.isExcluded(code)).thenReturn("rozetked".equals(code));
        }

        sourceBootstrapService.seedSources();

        verify(sourceRepository, times(4)).save(any(Source.class));
    }

    @Test
    void shouldSeedMissingBuiltInSourceWhenNotExcluded() {
        for (String code : BUILT_IN_CODES) {
            when(sourceRepository.findByCode(code)).thenReturn(Optional.empty());
            when(sourceSeedExclusionService.isExcluded(code)).thenReturn(false);
        }

        sourceBootstrapService.seedSources();

        ArgumentCaptor<Source> captor = ArgumentCaptor.forClass(Source.class);
        verify(sourceRepository, times(5)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(Source::getCode).contains("rozetked");
    }
}
