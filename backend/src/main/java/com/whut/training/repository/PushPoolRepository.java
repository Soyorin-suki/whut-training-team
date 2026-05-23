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
import java.util.List;
import java.util.Optional;

@Repository
public class PushPoolRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<PushPoolItem> poolItemRowMapper = (rs, rowNum) -> {
        Timestamp ca = rs.getTimestamp("created_at");
        Timestamp aa = rs.getTimestamp("approved_at");
        return new PushPoolItem(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("link"),
                rs.getString("description"),
                rs.getLong("submitter_id"),
                rs.getString("status"),
                rs.getInt("sort_order"),
                ca != null ? ca.toLocalDateTime() : null,
                (Long) rs.getObject("approved_by"),
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
        return new PushPoolItem(id, title, link, description, submitterId, "PENDING", null, now.toLocalDateTime(), null, null);
    }

    public Optional<PushPoolItem> findById(Long id) {
        List<PushPoolItem> rows = jdbcTemplate.query(
                "SELECT id, title, link, description, submitter_id, status, sort_order, created_at, approved_by, approved_at FROM push_pool WHERE id = ?",
                poolItemRowMapper, id
        );
        return rows.stream().findFirst();
    }

    public List<PushPoolItem> findAllByStatus(String status) {
        return jdbcTemplate.query(
                "SELECT id, title, link, description, submitter_id, status, sort_order, created_at, approved_by, approved_at FROM push_pool WHERE status = ? ORDER BY sort_order ASC",
                poolItemRowMapper, status
        );
    }

    public List<PushPoolItem> findAll() {
        return jdbcTemplate.query(
                "SELECT id, title, link, description, submitter_id, status, sort_order, created_at, approved_by, approved_at FROM push_pool ORDER BY sort_order ASC",
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
                "SELECT id, title, link, description, submitter_id, status, sort_order, created_at, approved_by, approved_at FROM push_pool WHERE status = 'APPROVED' ORDER BY sort_order ASC LIMIT 1",
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
                "INSERT OR REPLACE INTO daily_push (date, push_id) VALUES (?, ?)",
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
}
