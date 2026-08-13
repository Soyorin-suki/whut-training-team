package com.whut.training.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

@Component
@Profile("prod")
public class ProductionSecurityValidator {

    private static final Set<String> FORBIDDEN_SECRETS = Set.of(
            "123456", "password", "root", "admin", "superadmin123",
            "replace-with-a-long-random-password",
            "replace-with-a-different-long-random-password"
    );

    private final String mysqlPassword;
    private final String superAdminUsername;
    private final String superAdminPassword;
    private final String allowedOrigins;

    public ProductionSecurityValidator(
            @Value("${spring.datasource.password}") String mysqlPassword,
            @Value("${superAdmin.username}") String superAdminUsername,
            @Value("${superAdmin.password}") String superAdminPassword,
            @Value("${app.cors.allowed-origins}") String allowedOrigins
    ) {
        this.mysqlPassword = mysqlPassword;
        this.superAdminUsername = superAdminUsername;
        this.superAdminPassword = superAdminPassword;
        this.allowedOrigins = allowedOrigins;
    }

    @PostConstruct
    public void validate() {
        requireStrongSecret(mysqlPassword, "MYSQL_PASSWORD");
        requireStrongSecret(superAdminPassword, "SUPERADMIN_PASSWORD");
        if (superAdminUsername == null || superAdminUsername.isBlank()
                || "superadmin".equalsIgnoreCase(superAdminUsername.trim())
                || superAdminUsername.toLowerCase(Locale.ROOT).startsWith("replace-with")) {
            throw new IllegalStateException("SUPERADMIN_USERNAME must be a private, non-default production account name");
        }
        String[] origins = Arrays.stream(allowedOrigins == null ? new String[0] : allowedOrigins.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toArray(String[]::new);
        if (origins.length == 0 || Arrays.stream(origins).anyMatch(origin ->
                "*".equals(origin)
                        || !origin.startsWith("https://")
                        || origin.contains("localhost")
                        || origin.contains("127.0.0.1"))) {
            throw new IllegalStateException("CORS_ALLOWED_ORIGINS must contain only explicit HTTPS production origins");
        }
    }

    private void requireStrongSecret(String value, String name) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() < 16 || FORBIDDEN_SECRETS.contains(normalized.toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException(name + " must contain at least 16 non-default characters");
        }
    }
}
