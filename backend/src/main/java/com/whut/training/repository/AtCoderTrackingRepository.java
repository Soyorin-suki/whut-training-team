package com.whut.training.repository;

import com.whut.training.domain.dto.AtCoderAbcDashboard;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class AtCoderTrackingRepository {
    private final JdbcTemplate jdbcTemplate;

    public AtCoderTrackingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsertContest(String contestId, String name, long start, long end, String url) {
        jdbcTemplate.update("""
                INSERT INTO atcoder_contest
                    (contest_id, name, start_time_seconds, end_time_seconds, contest_url, discovered_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE name = VALUES(name), start_time_seconds = VALUES(start_time_seconds),
                    end_time_seconds = VALUES(end_time_seconds), contest_url = VALUES(contest_url)
                """, contestId, name, start, end, url, OffsetDateTime.now().toString());
    }

    public Optional<ContestRow> findContest(String contestId) {
        return jdbcTemplate.query("SELECT * FROM atcoder_contest WHERE contest_id = ?", contestMapper(), contestId)
                .stream().findFirst();
    }

    public List<ContestRow> findRecentContests(int limit) {
        return jdbcTemplate.query("SELECT * FROM atcoder_contest ORDER BY start_time_seconds DESC LIMIT ?", contestMapper(), limit);
    }

    public List<ContestRow> findSyncCandidates(long earliestStartSeconds) {
        return jdbcTemplate.query(
                "SELECT * FROM atcoder_contest WHERE start_time_seconds >= ? ORDER BY start_time_seconds DESC",
                contestMapper(), earliestStartSeconds);
    }

    public void freezeActiveMembers(String contestId) {
        String now = OffsetDateTime.now().toString();
        jdbcTemplate.update("""
                INSERT INTO atcoder_requirement_member
                    (contest_id, user_id, atcoder_handle_snapshot, required, exempted, frozen_at)
                SELECT ?, id, atcoder_handle, 1, 0, ? FROM users WHERE member_type = 'ACTIVE_TEAM'
                ON DUPLICATE KEY UPDATE contest_id = VALUES(contest_id)
                """, contestId, now);
        jdbcTemplate.update("UPDATE atcoder_contest SET member_snapshot_frozen = 1 WHERE contest_id = ?", contestId);
    }

    /** Keeps an upcoming contest preview aligned with the current active-team roster until kickoff. */
    public void prepareActiveMembers(String contestId) {
        jdbcTemplate.update("""
                DELETE FROM atcoder_requirement_member
                WHERE contest_id = ? AND user_id NOT IN (
                    SELECT id FROM users WHERE member_type = 'ACTIVE_TEAM'
                )
                """, contestId);
        jdbcTemplate.update("""
                INSERT INTO atcoder_requirement_member
                    (contest_id, user_id, atcoder_handle_snapshot, required, exempted, frozen_at)
                SELECT ?, id, atcoder_handle, 1, 0, ? FROM users WHERE member_type = 'ACTIVE_TEAM'
                ON DUPLICATE KEY UPDATE atcoder_handle_snapshot = VALUES(atcoder_handle_snapshot)
                """, contestId, OffsetDateTime.now().toString());
    }

    public List<RequirementRow> findRequirements(String contestId) {
        return jdbcTemplate.query("""
                SELECT r.contest_id, r.user_id, COALESCE(r.atcoder_handle_snapshot, u.atcoder_handle) AS atcoder_handle_snapshot,
                       r.required, r.exempted,
                       r.exemption_reason, u.username, u.display_name, u.avatar_url,
                       p.participated, p.contest_rank, p.performance, p.is_rated, p.old_rating,
                       p.new_rating, p.ac_count, p.solved_problem_ids, p.compliance_status,
                       p.source_error, p.checked_at
                FROM atcoder_requirement_member r
                INNER JOIN users u ON u.id = r.user_id
                LEFT JOIN atcoder_participation p ON p.contest_id = r.contest_id AND p.user_id = r.user_id
                WHERE r.contest_id = ? AND r.required = 1
                ORDER BY u.username ASC
                """, (rs, rowNum) -> new RequirementRow(
                rs.getString("contest_id"), rs.getLong("user_id"), rs.getString("username"),
                rs.getString("display_name"), rs.getString("avatar_url"),
                rs.getString("atcoder_handle_snapshot"), bool(rs.getObject("exempted")),
                rs.getString("exemption_reason"), bool(rs.getObject("participated")),
                (Integer) rs.getObject("contest_rank"), (Integer) rs.getObject("performance"),
                nullableBool(rs.getObject("is_rated")), (Integer) rs.getObject("old_rating"),
                (Integer) rs.getObject("new_rating"), (Integer) rs.getObject("ac_count"),
                rs.getString("solved_problem_ids"), rs.getString("compliance_status"),
                rs.getString("source_error"), rs.getString("checked_at")
        ), contestId);
    }

    public void upsertParticipation(String contestId, Long userId, boolean participated, Integer rank,
                                    Integer performance, Boolean rated, Integer oldRating, Integer newRating,
                                    Integer acCount, String solvedProblemIds, String status, String error) {
        jdbcTemplate.update("""
                INSERT INTO atcoder_participation
                    (contest_id, user_id, participated, contest_rank, performance, is_rated, old_rating,
                     new_rating, ac_count, solved_problem_ids, compliance_status, source_error, checked_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE participated = VALUES(participated), contest_rank = VALUES(contest_rank),
                    performance = VALUES(performance), is_rated = VALUES(is_rated), old_rating = VALUES(old_rating),
                    new_rating = VALUES(new_rating), ac_count = VALUES(ac_count),
                    solved_problem_ids = VALUES(solved_problem_ids), compliance_status = VALUES(compliance_status),
                    source_error = VALUES(source_error), checked_at = VALUES(checked_at)
                """, contestId, userId, participated, rank, performance, rated, oldRating, newRating,
                acCount, solvedProblemIds, status, error, OffsetDateTime.now().toString());
    }

    public AtCoderAbcDashboard.TrackingSetting getSetting() {
        return jdbcTemplate.query("SELECT minimum_ac_count, grace_hours FROM atcoder_tracking_setting WHERE id = 1",
                (rs, rowNum) -> new AtCoderAbcDashboard.TrackingSetting(
                        rs.getInt("minimum_ac_count"), rs.getInt("grace_hours")))
                .stream().findFirst().orElse(new AtCoderAbcDashboard.TrackingSetting(1, 24));
    }

    public void updateSetting(int minimumAcCount, int graceHours, Long updatedBy) {
        jdbcTemplate.update("""
                UPDATE atcoder_tracking_setting SET minimum_ac_count = ?, grace_hours = ?,
                    updated_at = ?, updated_by = ? WHERE id = 1
                """, minimumAcCount, graceHours, OffsetDateTime.now().toString(), updatedBy);
        jdbcTemplate.update("""
                UPDATE atcoder_participation
                SET compliance_status = CASE
                    WHEN participated = 1 AND ac_count IS NOT NULL AND ac_count >= ? THEN 'COMPLETED'
                    WHEN participated = 1 AND ac_count IS NOT NULL THEN 'PARTICIPATED'
                    ELSE compliance_status END
                """, minimumAcCount);
    }

    public void setExemption(String contestId, Long userId, boolean exempted, String reason) {
        jdbcTemplate.update("""
                UPDATE atcoder_requirement_member SET exempted = ?, exemption_reason = ?
                WHERE contest_id = ? AND user_id = ?
                """, exempted, exempted ? reason : null, contestId, userId);
    }

    public void markContestSynced(String contestId, String status) {
        jdbcTemplate.update("UPDATE atcoder_contest SET sync_status = ?, last_sync_at = ? WHERE contest_id = ?",
                status, OffsetDateTime.now().toString(), contestId);
    }

    public List<AtCoderExportRow> findExportRows(Long startTimeSeconds) {
        String filter = startTimeSeconds == null ? "" : " AND c.start_time_seconds >= ?";
        String sql = """
                SELECT c.contest_id, c.name, c.start_time_seconds, c.contest_url,
                       r.user_id, u.username, u.display_name, r.atcoder_handle_snapshot,
                       r.exempted, r.exemption_reason, p.compliance_status, p.participated,
                       p.ac_count, p.solved_problem_ids, p.contest_rank, p.performance,
                       p.is_rated, p.old_rating, p.new_rating, p.checked_at
                FROM atcoder_contest c
                INNER JOIN atcoder_requirement_member r ON r.contest_id = c.contest_id
                INNER JOIN users u ON u.id = r.user_id
                LEFT JOIN atcoder_participation p ON p.contest_id = r.contest_id AND p.user_id = r.user_id
                WHERE r.required = 1
                """ + filter + " ORDER BY c.start_time_seconds DESC, u.username ASC";
        Object[] args = startTimeSeconds == null ? new Object[0] : new Object[]{startTimeSeconds};
        return jdbcTemplate.query(sql, (rs, rowNum) -> new AtCoderExportRow(
                rs.getString("contest_id"), rs.getString("name"), rs.getLong("start_time_seconds"),
                rs.getString("contest_url"), rs.getLong("user_id"), rs.getString("username"),
                rs.getString("display_name"), rs.getString("atcoder_handle_snapshot"),
                bool(rs.getObject("exempted")), rs.getString("exemption_reason"),
                rs.getString("compliance_status"), bool(rs.getObject("participated")),
                (Integer) rs.getObject("ac_count"), rs.getString("solved_problem_ids"),
                (Integer) rs.getObject("contest_rank"), (Integer) rs.getObject("performance"),
                nullableBool(rs.getObject("is_rated")), (Integer) rs.getObject("old_rating"),
                (Integer) rs.getObject("new_rating"), rs.getString("checked_at")
        ), args);
    }

    private org.springframework.jdbc.core.RowMapper<ContestRow> contestMapper() {
        return (rs, rowNum) -> new ContestRow(
                rs.getString("contest_id"), rs.getString("name"), rs.getLong("start_time_seconds"),
                rs.getLong("end_time_seconds"), rs.getString("contest_url"),
                bool(rs.getObject("member_snapshot_frozen")), rs.getString("sync_status"),
                rs.getString("last_sync_at"));
    }

    private static boolean bool(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.intValue() != 0;
        return Boolean.parseBoolean(value.toString());
    }

    private static Boolean nullableBool(Object value) {
        return value == null ? null : bool(value);
    }

    public record ContestRow(String contestId, String name, long startTimeSeconds, long endTimeSeconds,
                             String contestUrl, boolean memberSnapshotFrozen, String syncStatus, String lastSyncAt) {}
    public record RequirementRow(String contestId, Long userId, String username, String displayName,
                                 String avatarUrl, String atcoderHandle, boolean exempted, String exemptionReason,
                                 boolean participated, Integer contestRank, Integer performance, Boolean rated,
                                 Integer oldRating, Integer newRating, Integer acCount, String solvedProblemIds,
                                 String status, String sourceError, String checkedAt) {}
    public record AtCoderExportRow(String contestId, String contestName, long startTimeSeconds, String contestUrl,
                                   Long userId, String username, String displayName, String atcoderHandle,
                                   boolean exempted, String exemptionReason, String status, boolean participated,
                                   Integer acCount, String solvedProblemIds, Integer contestRank, Integer performance,
                                   Boolean rated, Integer oldRating, Integer newRating, String checkedAt) {}
}
