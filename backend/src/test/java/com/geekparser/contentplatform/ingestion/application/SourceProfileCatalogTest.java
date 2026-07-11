package com.geekparser.contentplatform.ingestion.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SourceProfileCatalogTest {

    @Test
    void shouldContainConfiguredSources() {
        SourceProfileCatalog catalog = new SourceProfileCatalog();

        assertThat(catalog.defaults()).hasSize(5);
        assertThat(catalog.findByCode("rozetked")).isPresent();
        assertThat(catalog.findByCode("macrumors")).isPresent();
        assertThat(catalog.findByCode("3dnews")).isPresent();
    }
}
