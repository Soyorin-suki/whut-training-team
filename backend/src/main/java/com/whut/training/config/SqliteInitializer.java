package com.whut.training.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * SQLite 数据库初始化器。
 *
 * <p>应用启动时负责创建目录、初始化表结构和通过 PRAGMA 补充缺失列。
 * 仅当 {@code app.database.type=sqlite}（默认）时激活。
 * 实现 {@link DatabaseInitializer} 接口，可通过替换实现类切换数据库。
 */
@Component
@ConditionalOnProperty(name = "app.database.type", havingValue = "sqlite", matchIfMissing = true)
public class SqliteInitializer implements DatabaseInitializer {

    private final JdbcTemplate jdbcTemplate;
    private final DataSourceProperties dataSourceProperties;

    /**
     * 创建 SQLite 初始化器。
     *
     * @param jdbcTemplate         JDBC 模板。
     * @param dataSourceProperties 数据源配置。
     */
    public SqliteInitializer(JdbcTemplate jdbcTemplate, DataSourceProperties dataSourceProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSourceProperties = dataSourceProperties;
    }

    /**
     * 初始化数据库目录和表结构。
     *
     * @throws IOException 当数据库文件目录创建失败时抛出。
     */
    @Override
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
                    total_points INTEGER DEFAULT 0,
                    display_name TEXT,
                    bio TEXT
                )
                """);
        ensureColumnExists("users", "uid", "INTEGER");
        ensureColumnExists("users", "codeforces_rating", "INTEGER");
        ensureColumnExists("users", "max_rating", "INTEGER");
        ensureColumnExists("users", "is_online", "INTEGER");
        ensureColumnExists("users", "last_online_time_seconds", "INTEGER");
        ensureColumnExists("users", "avatar_url", "TEXT");
        ensureColumnExists("users", "total_points", "INTEGER DEFAULT 0");
        ensureColumnExists("users", "display_name", "TEXT");
        ensureColumnExists("users", "bio", "TEXT");

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
            CREATE TABLE IF NOT EXISTS daily_problem_slot (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date TEXT NOT NULL,
                slot TEXT NOT NULL,
                problem_key TEXT NOT NULL,
                contest_id INTEGER NOT NULL,
                problem_index TEXT NOT NULL,
                name TEXT NOT NULL,
                rating INTEGER,
                tags TEXT,
                source_url TEXT NOT NULL,
                generated_at TEXT NOT NULL,
                generated_by TEXT NOT NULL,
                is_redrawn INTEGER NOT NULL DEFAULT 0
            )
            """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_daily_problem_slot_date ON daily_problem_slot(date)");

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
                CREATE TABLE IF NOT EXISTS user_daily_slot_status (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    date TEXT NOT NULL,
                    slot TEXT NOT NULL,
                    problem_key TEXT NOT NULL,
                    submission_id INTEGER NOT NULL,
                    verdict TEXT NOT NULL,
                    checked_at TEXT NOT NULL,
                    score INTEGER NOT NULL DEFAULT 0,
                    UNIQUE(user_id, date, problem_key)
                )
                """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_user_daily_slot_status_user_date ON user_daily_slot_status(user_id, date)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_user_daily_slot_status_problem ON user_daily_slot_status(problem_key)");

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

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS push_pool (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    link TEXT NOT NULL,
                    description TEXT,
                    submitter_id INTEGER NOT NULL,
                    status TEXT NOT NULL DEFAULT 'PENDING',
                    sort_order INTEGER,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    approved_by INTEGER,
                    approved_at DATETIME
                )
                """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_push_pool_status ON push_pool(status)");

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS push_submissions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    push_id INTEGER NOT NULL,
                    user_id INTEGER NOT NULL,
                    submission_link TEXT NOT NULL,
                    result_description TEXT,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS daily_push (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    date TEXT NOT NULL UNIQUE,
                    push_id INTEGER NOT NULL
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS role_change_log (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    target_user_id INTEGER NOT NULL,
                    changed_by INTEGER NOT NULL,
                    from_role TEXT,
                    to_role TEXT NOT NULL,
                    changed_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
                """);

        // 数据迁移：将已推送但状态仍为 APPROVED 的历史题目标记为 PUBLISHED
        jdbcTemplate.update("""
                UPDATE push_pool SET status = 'PUBLISHED'
                WHERE status = 'APPROVED' AND id IN (SELECT push_id FROM daily_push)
                """);
    }

    /**
     * 如果 SQLite 文件位于本地路径，则确保父目录存在。
     *
     * @throws IOException 目录创建失败时抛出。
     */
    private void createSqliteParentDirIfNeeded() throws IOException {
        String url = dataSourceProperties.getUrl();
        if (url == null || !url.startsWith("jdbc:sqlite:")) {
            return;
        }

        String afterPrefix = url.substring("jdbc:sqlite:".length());
        // 剥离查询参数（如 ?busy_timeout=5000），避免将其误认为文件名的一部分
        int queryIdx = afterPrefix.indexOf('?');
        String dbPath = queryIdx >= 0 ? afterPrefix.substring(0, queryIdx) : afterPrefix;
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

    /**
     * 为空表补充缺失列。
     *
     * @param tableName  表名。
     * @param columnName 列名。
     * @param columnType 列类型。
     */
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
