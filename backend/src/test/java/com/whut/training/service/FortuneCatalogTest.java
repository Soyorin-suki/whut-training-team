package com.whut.training.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class FortuneCatalogTest {

    @Test
    void loadsCatalogAndSelectsStableFortuneForTheSameUserAndDate() {
        FortuneCatalog catalog = new FortuneCatalog(
                new ObjectMapper(),
                new ClassPathResource("fortunes.json")
        );

        FortuneCatalog.Fortune first = catalog.select(2L, LocalDate.of(2026, 8, 11));
        FortuneCatalog.Fortune repeated = catalog.select(2L, LocalDate.of(2026, 8, 11));

        assertThat(catalog.size()).isGreaterThanOrEqualTo(40);
        assertThat(repeated).isEqualTo(first);
        assertThat(first.message()).isNotBlank();
    }
}
