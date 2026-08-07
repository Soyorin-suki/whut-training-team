package com.whut.training.repository;

import com.whut.training.service.CodeforcesApiService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Codeforces rated 比赛完整历史的本地存储。
 */
@Repository
public class CodeforcesRatingHistoryRepository {

    private final JdbcTemplate jdbcTemplate;

    public CodeforcesRatingHistoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean existsByUserId(Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM cf_rating_change WHERE user_id = ?",
                Integer.class,
                userId
        );
        return count != null && count > 0;
    }

    public void replaceForUser(Long userId, List<CodeforcesApiService.CodeforcesRatingChange> changes) {
        jdbcTemplate.update("DELETE FROM cf_rating_change WHERE user_id = ?", userId);
        List<CodeforcesApiService.CodeforcesRatingChange> validChanges = changes.stream()
                .filter(change -> change.contestId() != null)
                .toList();
        if (validChanges.isEmpty()) return;

        jdbcTemplate.batchUpdate(
                """
                INSERT INTO cf_rating_change (
                    user_id, contest_id, contest_name, contest_rank, old_rating, new_rating,
                    rating_change, rating_update_time_seconds, contest_url
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                validChanges,
                validChanges.size(),
                (statement, change) -> {
                    statement.setLong(1, userId);
                    statement.setLong(2, change.contestId());
                    statement.setString(3, change.contestName());
                    statement.setObject(4, change.rank());
                    statement.setObject(5, change.oldRating());
                    statement.setObject(6, change.newRating());
                    Integer delta = change.oldRating() == null || change.newRating() == null
                            ? null
                            : change.newRating() - change.oldRating();
                    statement.setObject(7, delta);
                    statement.setObject(8, change.ratingUpdateTimeSeconds());
                    statement.setString(9, "https://codeforces.com/contest/" + change.contestId());
                }
        );
    }

    public List<RatingContestExportRow> findActiveTeamHistory(Long startTimeSeconds) {
        String dateFilter = startTimeSeconds == null ? "" : " AND c.rating_update_time_seconds >= ?";
        String sql = """
                SELECT u.id AS user_id,
                       u.username,
                       u.display_name,
                       u.codeforces_handle,
                       c.contest_id,
                       c.contest_name,
                       c.contest_rank,
                       c.old_rating,
                       c.new_rating,
                       c.rating_change,
                       c.rating_update_time_seconds,
                       c.contest_url
                FROM cf_rating_change c
                INNER JOIN users u ON u.id = c.user_id
                WHERE u.member_type = 'ACTIVE_TEAM'
                """ + dateFilter + " ORDER BY c.rating_update_time_seconds DESC, u.username ASC";

        Object[] arguments = startTimeSeconds == null ? new Object[0] : new Object[]{startTimeSeconds};
        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new RatingContestExportRow(
                        rs.getLong("user_id"),
                        rs.getString("username"),
                        rs.getString("display_name"),
                        rs.getString("codeforces_handle"),
                        rs.getLong("contest_id"),
                        rs.getString("contest_name"),
                        (Integer) rs.getObject("contest_rank"),
                        (Integer) rs.getObject("old_rating"),
                        (Integer) rs.getObject("new_rating"),
                        (Integer) rs.getObject("rating_change"),
                        (Long) rs.getObject("rating_update_time_seconds"),
                        rs.getString("contest_url")
                ),
                arguments
        );
    }

    public record RatingContestExportRow(
            Long userId,
            String username,
            String displayName,
            String codeforcesHandle,
            Long contestId,
            String contestName,
            Integer rank,
            Integer oldRating,
            Integer newRating,
            Integer ratingChange,
            Long ratingUpdateTimeSeconds,
            String contestUrl
    ) {
    }
}
