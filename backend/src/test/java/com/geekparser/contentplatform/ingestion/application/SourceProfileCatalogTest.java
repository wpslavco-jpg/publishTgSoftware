package com.geekparser.contentplatform.ingestion.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SourceProfileCatalogTest {

    @Test
    void shouldContainConfiguredSources() {
        SourceProfileCatalog catalog = new SourceProfileCatalog();

        assertThat(catalog.defaults()).hasSize(5);
        assertThat(catalog.findBuiltInByCode("rozetked")).isPresent();
        assertThat(catalog.findBuiltInByCode("macrumors")).isPresent();
        assertThat(catalog.findBuiltInByCode("3dnews")).isPresent();
    }

    @Test
    void shouldParsePatternsFromMultilineText() {
        assertThat(SourceProfileCatalog.parsePatterns(".+/news/.+\nhttps://example\\.com/.+")).hasSize(2);
    }
}
