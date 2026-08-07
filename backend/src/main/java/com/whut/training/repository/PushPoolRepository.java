package com.whut.training.repository;

import com.whut.training.domain.entity.PushPoolItem;
import com.whut.training.domain.entity.PushSubmission;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class PushPoolRepository {

    private final JdbcTemplate jdbcTemplate;

    /** push_pool LEFT JOIN users 的公共 SELECT 片段，统一获取 submitter_username */
    private static final String SELECT_WITH_USER =
            "SELECT pp.id, pp.title, pp.link, pp.description, pp.submitter_id,"
            + " u.username AS submitter_username, pp.status, pp.sort_order,"
            + " pp.created_at, pp.approved_by, pp.approved_at"
            + " FROM push_pool pp LEFT JOIN users u ON pp.submitter_id = u.id";

    private final RowMapper<PushPoolItem> poolItemRowMapper = (rs, rowNum) -> {
        Timestamp ca = rs.getTimestamp("created_at");
        Timestamp aa = rs.getTimestamp("approved_at");
        Long approvedBy = rs.getLong("approved_by");
        if (rs.wasNull()) approvedBy = null;
        return new PushPoolItem(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("link"),
                rs.getString("description"),
                rs.getLong("submitter_id"),
                rs.getString("submitter_username"),
                rs.getString("status"),
                rs.getInt("sort_order"),
                ca != null ? ca.toLocalDateTime() : null,
                approvedBy,
                aa != null ? aa.toLocalDateTime() : null
        );
    };

    private final RowMapper<PushSubmission> submissionRowMapper = (rs, rowNum) -> {
        Timestamp ca = rs.getTimestamp("created_at");
        return new PushSubmission(
                rs.getLong("id"),
                rs.getLong("push_id"),
                rs.getLong("user_id"),
                rs.getString("submission_link"),
                rs.getString("result_description"),
                ca != null ? ca.toLocalDateTime() : null
        );
    };

    public PushPoolRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PushPoolItem insert(String title, String link, String description, Long submitterId) {
        String sql = "INSERT INTO push_pool (title, link, description, submitter_id, status, sort_order, created_at) VALUES (?, ?, ?, ?, 'PENDING', (SELECT COALESCE(MAX(sort_order), 0) + 1 FROM push_pool), ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        jdbcTemplate.update(connection -> {
            var stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, title);
            stmt.setString(2, link);
            stmt.setString(3, description);
            stmt.setLong(4, submitterId);
            stmt.setTimestamp(5, now);
            return stmt;
        }, keyHolder);
        Long id = keyHolder.getKey() == null ? null : keyHolder.getKey().longValue();
        return new PushPoolItem(id, title, link, description, submitterId, null, "PENDING", null, now.toLocalDateTime(), null, null);
    }

    public Optional<PushPoolItem> findById(Long id) {
        List<PushPoolItem> rows = jdbcTemplate.query(
                SELECT_WITH_USER + " WHERE pp.id = ?",
                poolItemRowMapper, id
        );
        return rows.stream().findFirst();
    }

    public List<PushPoolItem> findAllByStatus(String status) {
        return jdbcTemplate.query(
                SELECT_WITH_USER + " WHERE pp.status = ? ORDER BY pp.sort_order ASC",
                poolItemRowMapper, status
        );
    }

    public List<PushPoolItem> findAllBySubmitter(Long submitterId) {
        return jdbcTemplate.query(
                SELECT_WITH_USER + " WHERE pp.submitter_id = ? ORDER BY pp.created_at DESC",
                poolItemRowMapper, submitterId
        );
    }

    public List<PushPoolItem> findAll() {
        return jdbcTemplate.query(
                SELECT_WITH_USER + " ORDER BY pp.sort_order ASC",
                poolItemRowMapper
        );
    }

    /** 查询所有已推送的题目（在 daily_push 表中），按推送日期降序 */
    public List<PushPoolItem> findPublished() {
        return jdbcTemplate.query(
                SELECT_WITH_USER + " INNER JOIN daily_push dp ON pp.id = dp.push_id ORDER BY dp.date DESC",
                poolItemRowMapper
        );
    }

    public void updateStatus(Long id, String status, Long approvedBy) {
        jdbcTemplate.update(
                "UPDATE push_pool SET status = ?, approved_by = ?, approved_at = ? WHERE id = ?",
                status, approvedBy, Timestamp.valueOf(LocalDateTime.now()), id
        );
    }

    public void promoteToFront(Long id) {
        jdbcTemplate.update("UPDATE push_pool SET sort_order = (SELECT MIN(sort_order) - 1 FROM push_pool) WHERE id = ?", id);
    }

    public Optional<PushPoolItem> popNextApproved() {
        List<PushPoolItem> rows = jdbcTemplate.query(
                SELECT_WITH_USER + " WHERE pp.status = 'APPROVED' ORDER BY pp.sort_order ASC LIMIT 1",
                poolItemRowMapper
        );
        return rows.stream().findFirst();
    }

    public void updateSortOrder(Long id, int sortOrder) {
        jdbcTemplate.update("UPDATE push_pool SET sort_order = ? WHERE id = ?", sortOrder, id);
    }

    // --- submissions ---

    public PushSubmission insertSubmission(Long pushId, Long userId, String submissionLink, String resultDescription) {
        String sql = "INSERT INTO push_submissions (push_id, user_id, submission_link, result_description, created_at) VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        jdbcTemplate.update(connection -> {
            var stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setLong(1, pushId);
            stmt.setLong(2, userId);
            stmt.setString(3, submissionLink);
            stmt.setString(4, resultDescription);
            stmt.setTimestamp(5, now);
            return stmt;
        }, keyHolder);
        Long id = keyHolder.getKey() == null ? null : keyHolder.getKey().longValue();
        return new PushSubmission(id, pushId, userId, submissionLink, resultDescription, now.toLocalDateTime());
    }

    public List<PushSubmission> findSubmissionsByPushId(Long pushId) {
        return jdbcTemplate.query(
                "SELECT id, push_id, user_id, submission_link, result_description, created_at FROM push_submissions WHERE push_id = ? ORDER BY created_at DESC",
                submissionRowMapper, pushId
        );
    }

    public List<PushSubmission> findSubmissionsByUserId(Long userId) {
        return jdbcTemplate.query(
                "SELECT id, push_id, user_id, submission_link, result_description, created_at FROM push_submissions WHERE user_id = ? ORDER BY created_at DESC",
                submissionRowMapper, userId
        );
    }

    // --- daily push ---

    public void setDailyPush(LocalDate date, Long pushId) {
        jdbcTemplate.update(
                "INSERT INTO daily_push (date, push_id) VALUES (?, ?) " +
                        "ON DUPLICATE KEY UPDATE push_id = VALUES(push_id)",
                date.toString(), pushId
        );
    }

    public Optional<Long> findDailyPushId(LocalDate date) {
        List<Long> rows = jdbcTemplate.query(
                "SELECT push_id FROM daily_push WHERE date = ?",
                (rs, rowNum) -> rs.getLong("push_id"),
                date.toString()
        );
        return rows.stream().findFirst();
    }

    public boolean existsInDailyPush(Long pushId) {
        Integer c = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM daily_push WHERE push_id = ?",
                Integer.class, pushId
        );
        return c != null && c > 0;
    }

    public boolean deleteById(Long id) {
        int rows = jdbcTemplate.update("DELETE FROM push_pool WHERE id = ?", id);
        return rows > 0;
    }

    /**
     * 查询所有已审核但尚未推送的题目（status = APPROVED）。
     * PUBLISHED 状态的题目已被推送，不在此列。
     */
    public List<PushPoolItem> findApprovedUnpublished() {
        return jdbcTemplate.query(
                SELECT_WITH_USER + " WHERE pp.status = 'APPROVED' ORDER BY pp.sort_order ASC",
                poolItemRowMapper
        );
    }

    /**
     * Query push history: daily_push JOIN push_pool, ordered by date DESC.
     */
    public List<Map<String, Object>> findPushedHistory() {
        return jdbcTemplate.query(
                "SELECT dp.date AS push_date, pp.id, pp.title, pp.link, pp.description, pp.submitter_id,"
                + " u.username AS submitter_username, pp.status, pp.sort_order, pp.created_at, pp.approved_by, pp.approved_at"
                + " FROM daily_push dp JOIN push_pool pp ON dp.push_id = pp.id"
                + " LEFT JOIN users u ON pp.submitter_id = u.id ORDER BY dp.date DESC",
                (rs, rowNum) -> {
                    Timestamp ca = rs.getTimestamp("created_at");
                    Timestamp aa = rs.getTimestamp("approved_at");
                    Map<String, Object> map = new java.util.LinkedHashMap<>();
                    map.put("pushDate", rs.getString("push_date"));
                    map.put("id", rs.getLong("id"));
                    map.put("title", rs.getString("title"));
                    map.put("link", rs.getString("link"));
                    map.put("description", rs.getString("description"));
                    map.put("submitterId", rs.getLong("submitter_id"));
                    map.put("submitterUsername", rs.getString("submitter_username"));
                    map.put("status", rs.getString("status"));
                    map.put("sortOrder", rs.getInt("sort_order"));
                    map.put("createdAt", ca != null ? ca.toLocalDateTime().toString() : null);
                    map.put("approvedBy", (Long) rs.getObject("approved_by"));
                    map.put("approvedAt", aa != null ? aa.toLocalDateTime().toString() : null);
                    return map;
                }
        );
    }
}
