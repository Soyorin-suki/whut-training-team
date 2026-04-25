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

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS cf_problem (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    problem_key TEXT NOT NULL UNIQUE,
                    contest_id INTEGER NOT NULL,
                    problem_index TEXT NOT NULL,
                    name TEXT NOT NULL,
                    rating INTEGER,
                    tags TEXT,
                    is_interactive INTEGER NOT NULL DEFAULT 0,
                    source_contest_id INTEGER,
                    solved_count INTEGER,
                    source_url TEXT NOT NULL,
                    last_synced_at TEXT NOT NULL
                )
                """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_cf_problem_rating ON cf_problem(rating)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_cf_problem_contest_idx ON cf_problem(contest_id, problem_index)");

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS daily_problem (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    date TEXT NOT NULL UNIQUE,
                    problem_key TEXT NOT NULL,
                    contest_id INTEGER NOT NULL,
                    problem_index TEXT NOT NULL,
                    name TEXT NOT NULL,
                    rating INTEGER,
                    tags TEXT,
                    source_url TEXT NOT NULL,
                    generated_at TEXT NOT NULL,
                    generated_by TEXT NOT NULL
                )
                """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_daily_problem_date ON daily_problem(date)");

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS user_daily_status (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    date TEXT NOT NULL,
                    submission_id INTEGER NOT NULL,
                    verdict TEXT NOT NULL,
                    checked_at TEXT NOT NULL,
                    score INTEGER NOT NULL DEFAULT 1,
                    UNIQUE(user_id, date)
                )
                """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_user_daily_status_user ON user_daily_status(user_id)");

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS user_practice_draw (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    draw_date TEXT NOT NULL,
                    problem_key TEXT NOT NULL,
                    contest_id INTEGER NOT NULL,
                    problem_index TEXT NOT NULL,
                    name TEXT NOT NULL,
                    rating INTEGER,
                    tags TEXT,
                    source_url TEXT NOT NULL,
                    drawn_at TEXT NOT NULL,
                    submission_id INTEGER,
                    verdict TEXT,
                    checked_at TEXT
                )
                """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_user_practice_draw_user_date ON user_practice_draw(user_id, draw_date)");

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS auth_token_session (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    access_token TEXT NOT NULL UNIQUE,
                    refresh_token TEXT NOT NULL UNIQUE,
                    access_expired_at_seconds INTEGER NOT NULL,
                    refresh_expired_at_seconds INTEGER NOT NULL,
                    created_at_seconds INTEGER NOT NULL
                )
                """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_auth_token_session_user ON auth_token_session(user_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_auth_token_session_access_exp ON auth_token_session(access_expired_at_seconds)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_auth_token_session_refresh_exp ON auth_token_session(refresh_expired_at_seconds)");
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
