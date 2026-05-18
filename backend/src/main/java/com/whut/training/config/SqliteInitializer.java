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
                    avatar_url TEXT,
                    score INTEGER NOT NULL DEFAULT 0,
                    solved_problem_count INTEGER NOT NULL DEFAULT 0,
                    hard_solved_problem_count INTEGER NOT NULL DEFAULT 0,
                    current_streak_days INTEGER NOT NULL DEFAULT 0,
                    longest_streak_days INTEGER NOT NULL DEFAULT 0
                )
                """);
        ensureColumnExists("users", "uid", "INTEGER");
        ensureColumnExists("users", "codeforces_rating", "INTEGER");
        ensureColumnExists("users", "max_rating", "INTEGER");
        ensureColumnExists("users", "is_online", "INTEGER");
        ensureColumnExists("users", "last_online_time_seconds", "INTEGER");
        ensureColumnExists("users", "avatar_url", "TEXT");
        ensureColumnExists("users", "score", "INTEGER");
        ensureColumnExists("users", "solved_problem_count", "INTEGER");
        ensureColumnExists("users", "hard_solved_problem_count", "INTEGER");
        ensureColumnExists("users", "current_streak_days", "INTEGER NOT NULL DEFAULT 0");
        ensureColumnExists("users", "longest_streak_days", "INTEGER NOT NULL DEFAULT 0");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_users_score ON users(score)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_users_solved_problem_count ON users(solved_problem_count)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_users_hard_solved_problem_count ON users(hard_solved_problem_count)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_users_current_streak_days ON users(current_streak_days)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_users_longest_streak_days ON users(longest_streak_days)");

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
                CREATE TABLE IF NOT EXISTS problem_like (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    problem_key TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    UNIQUE(user_id, problem_key)
                )
                """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_problem_like_problem_key ON problem_like(problem_key)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_problem_like_user_id ON problem_like(user_id)");

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS problem_favorite (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    problem_key TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    UNIQUE(user_id, problem_key)
                )
                """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_problem_favorite_problem_key ON problem_favorite(problem_key)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_problem_favorite_user_id ON problem_favorite(user_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_problem_favorite_created_at ON problem_favorite(created_at)");

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS daily_problem_comment (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    daily_problem_date TEXT NOT NULL,
                    problem_key TEXT NOT NULL,
                    user_id INTEGER NOT NULL,
                    reply_comment_id INTEGER,
                    content TEXT NOT NULL,
                    created_at TEXT NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_daily_problem_comment_daily_problem
                ON daily_problem_comment(daily_problem_date, problem_key)
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_daily_problem_comment_reply_comment_id
                ON daily_problem_comment(reply_comment_id)
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_daily_problem_comment_user_id
                ON daily_problem_comment(user_id)
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_daily_problem_comment_created_at
                ON daily_problem_comment(created_at)
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS problem_comment (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    problem_key TEXT NOT NULL,
                    user_id INTEGER NOT NULL,
                    reply_comment_id INTEGER,
                    content TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    legacy_comment_id INTEGER UNIQUE
                )
                """);
        ensureColumnExists("problem_comment", "legacy_comment_id", "INTEGER");
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_problem_comment_problem_key
                ON problem_comment(problem_key)
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_problem_comment_reply_comment_id
                ON problem_comment(reply_comment_id)
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_problem_comment_user_id
                ON problem_comment(user_id)
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_problem_comment_created_at
                ON problem_comment(created_at)
                """);

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

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS ai_problem_session (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    created_by INTEGER NOT NULL,
                    provider_key TEXT NOT NULL,
                    model_name TEXT NOT NULL,
                    target_rating INTEGER,
                    target_tags TEXT,
                    status TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL
                )
                """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_ai_problem_session_updated_at ON ai_problem_session(updated_at)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_ai_problem_session_status ON ai_problem_session(status)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_ai_problem_session_created_by ON ai_problem_session(created_by)");

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS ai_problem_message (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    session_id INTEGER NOT NULL,
                    role TEXT NOT NULL,
                    content TEXT NOT NULL,
                    prompt_context_json TEXT,
                    created_at TEXT NOT NULL
                )
                """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_ai_problem_message_session_id ON ai_problem_message(session_id)");

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS ai_problem_draft (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    session_id INTEGER NOT NULL UNIQUE,
                    current_version INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    statement_md TEXT NOT NULL,
                    input_spec_md TEXT NOT NULL,
                    output_spec_md TEXT NOT NULL,
                    constraint_md TEXT NOT NULL DEFAULT '',
                    hint_md TEXT,
                    rating INTEGER,
                    tags TEXT,
                    source_type TEXT NOT NULL,
                    checker_note_md TEXT,
                    normalized_problem_json TEXT,
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL
                )
                """);
        ensureColumnExists("ai_problem_draft", "constraint_md", "TEXT NOT NULL DEFAULT ''");
        ensureColumnExists("ai_problem_draft", "checker_note_md", "TEXT");
        ensureColumnExists("ai_problem_draft", "normalized_problem_json", "TEXT");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_ai_problem_draft_session_id ON ai_problem_draft(session_id)");

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS ai_problem_version (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    draft_id INTEGER NOT NULL,
                    version_no INTEGER NOT NULL,
                    generation_prompt TEXT NOT NULL,
                    assistant_message TEXT,
                    llm_response_json TEXT NOT NULL,
                    normalized_problem_json TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    UNIQUE(draft_id, version_no)
                )
                """);
        ensureColumnExists("ai_problem_version", "assistant_message", "TEXT");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_ai_problem_version_draft_id ON ai_problem_version(draft_id)");

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS ai_problem_artifact (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    version_id INTEGER NOT NULL,
                    artifact_type TEXT NOT NULL,
                    file_name TEXT NOT NULL,
                    relative_path TEXT NOT NULL,
                    content_type TEXT NOT NULL,
                    size_bytes INTEGER NOT NULL,
                    created_at TEXT NOT NULL
                )
                """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_ai_problem_artifact_version_id ON ai_problem_artifact(version_id)");
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
