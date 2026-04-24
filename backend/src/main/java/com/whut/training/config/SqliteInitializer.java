package com.whut.training.config;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class SqliteInitializer {

    private final JdbcTemplate jdbcTemplate;

    public SqliteInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(Path.of("data"));
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    email TEXT,
                    password TEXT NOT NULL,
                    role TEXT
                )
                """);
    }
}
