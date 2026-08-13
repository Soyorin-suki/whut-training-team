package com.whut.training.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductionSecurityValidatorTest {

    @Test
    void acceptsStrongProductionConfiguration() {
        var validator = new ProductionSecurityValidator(
                "database-secret-6Bz!2pQx", "private-root-owner",
                "administrator-secret-9Yt!7LmQ", "https://acm.example.com"
        );

        assertDoesNotThrow(validator::validate);
    }

    @Test
    void rejectsDefaultSecretsAndInsecureCors() {
        assertThrows(IllegalStateException.class, () ->
                new ProductionSecurityValidator(
                        "123456", "superadmin", "superadmin123", "http://localhost:5173"
                ).validate()
        );
    }
}
