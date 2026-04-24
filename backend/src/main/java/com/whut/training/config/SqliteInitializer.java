package com.whut.training.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Component
public class SqliteInitializer {

    private final JdbcTemplate jdbcTemplate;
    private final DataSourceProperties dataSourceProperties;

    public SqliteInitializer(JdbcTemplate jdbcTemplate, DataSourceProperties dataSourceProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSourceProperties = dataSourceProperties;
    }

    @PostConstruct
    public void init() throws IOException {
        createSqliteParentDirIfNeeded();
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    email TEXT,
                    password TEXT NOT NULL,
                    role TEXT,
                    uid INTEGER,
                    codeforces_rating INTEGER,
                    max_rating INTEGER,
                    is_online INTEGER,
                    last_online_time_seconds INTEGER,
                    avatar_url TEXT
                )
                """);
        ensureColumnExists("users", "uid", "INTEGER");
        ensureColumnExists("users", "codeforces_rating", "INTEGER");
        ensureColumnExists("users", "max_rating", "INTEGER");
        ensureColumnExists("users", "is_online", "INTEGER");
        ensureColumnExists("users", "last_online_time_seconds", "INTEGER");
        ensureColumnExists("users", "avatar_url", "TEXT");
    }

    private void createSqliteParentDirIfNeeded() throws IOException {
        String url = dataSourceProperties.getUrl();
        if (url == null || !url.startsWith("jdbc:sqlite:")) {
            return;
        }

        String dbPath = url.substring("jdbc:sqlite:".length());
        if (dbPath.isBlank()) {
            return;
        }

        Path path = Paths.get(dbPath);
        if (!path.isAbsolute()) {
            path = path.toAbsolutePath().normalize();
        }
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private void ensureColumnExists(String tableName, String columnName, String columnType) {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList("PRAGMA table_info(" + tableName + ")");
        boolean exists = columns.stream()
                .map(column -> column.get("name"))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .anyMatch(columnName::equalsIgnoreCase);
        if (!exists) {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType);
        }
    }
}
