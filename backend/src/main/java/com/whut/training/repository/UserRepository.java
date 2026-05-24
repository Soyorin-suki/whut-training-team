package com.whut.training.repository;

import com.whut.training.domain.dto.LeaderboardItem;
import com.whut.training.domain.entity.User;
import com.whut.training.domain.enums.UserRole;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final String SELECT_COLUMNS =
            "id, username, email, password, role, uid, codeforces_rating, max_rating, is_online, last_online_time_seconds, avatar_url, total_points, display_name, bio";

    private final RowMapper<User> userRowMapperWithPoints = (rs, rowNum) -> {
        User u = new User(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("password"),
                parseRole(rs.getString("role")),
                (Integer) rs.getObject("codeforces_rating"),
                (Integer) rs.getObject("max_rating"),
                parseOnline(rs.getObject("is_online")),
                parseLongValue(rs.getObject("last_online_time_seconds")),
                rs.getString("avatar_url"),
                parseLongValue(rs.getObject("uid"))
        );
        Object tp = rs.getObject("total_points");
        if (tp != null) {
            if (tp instanceof Number n) {
                u.setTotalPoints(n.intValue());
            } else {
                try {
                    u.setTotalPoints(Integer.parseInt(tp.toString()));
                } catch (Exception ignored) {
                }
            }
        }
        u.setDisplayName(rs.getString("display_name"));
        u.setBio(rs.getString("bio"));
        return u;
    };

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public User save(User user) {
        if (user.getId() == null) {
            jdbcTemplate.update(
                    "INSERT INTO users (username, email, password, role, uid, codeforces_rating, max_rating, is_online, last_online_time_seconds, avatar_url, total_points, display_name, bio) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    user.getUsername(),
                    user.getEmail(),
                    user.getPassword(),
                    user.getRole() == null ? null : user.getRole().name(),
                    user.getUid(),
                    user.getCodeforcesRating(),
                    user.getMaxRating(),
                    user.getOnline(),
                    user.getLastOnlineTimeSeconds(),
                    user.getAvatarUrl(),
                    user.getTotalPoints() == null ? 0 : user.getTotalPoints(),
                    user.getDisplayName(),
                    user.getBio()
            );
            Long id = jdbcTemplate.queryForObject(
                    "SELECT id FROM users WHERE username = ?",
                    Long.class,
                    user.getUsername()
            );
            user.setId(id);
            return user;
        }

        jdbcTemplate.update(
                "UPDATE users SET username = ?, email = ?, password = ?, role = ?, uid = ?, codeforces_rating = ?, max_rating = ?, is_online = ?, last_online_time_seconds = ?, avatar_url = ?, total_points = ?, display_name = ?, bio = ? WHERE id = ?",
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                user.getRole() == null ? null : user.getRole().name(),
                user.getUid(),
                user.getCodeforcesRating(),
                user.getMaxRating(),
                user.getOnline(),
                user.getLastOnlineTimeSeconds(),
                user.getAvatarUrl(),
                user.getTotalPoints() == null ? 0 : user.getTotalPoints(),
                user.getDisplayName(),
                user.getBio(),
                user.getId()
        );
        return user;
    }

    public void incrementTotalPoints(Long userId, int delta) {
        jdbcTemplate.update("UPDATE users SET total_points = total_points + ? WHERE id = ?", delta, userId);
    }

    public List<User> findAll() {
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM users",
                userRowMapperWithPoints
        );
    }

    public Optional<User> findById(Long id) {
        List<User> users = jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM users WHERE id = ?",
                userRowMapperWithPoints,
                id
        );
        return users.stream().findFirst();
    }

    public Optional<User> findByUsername(String username) {
        List<User> users = jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM users WHERE username = ?",
                userRowMapperWithPoints,
                username
        );
        return users.stream().findFirst();
    }

    public int countAll() {
        Integer c = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM users", Integer.class);
        return c == null ? 0 : c;
    }

    public boolean existsByUsername(String username) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM users WHERE username = ?",
                Integer.class,
                username
        );
        return count != null && count > 0;
    }

    public int countTotal() {
        Integer c = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM users", Integer.class);
        return c == null ? 0 : c;
    }

    private RowMapper<LeaderboardItem> leaderboardRowMapper() {
        return (rs, rowNum) -> {
            LeaderboardItem it = new LeaderboardItem();
            it.setUserId(rs.getLong("id"));
            it.setUsername(rs.getString("username"));
            it.setTotalPoints(rs.getInt("total_points"));
            // Read as String to avoid SQLite JDBC nanosecond parsing bug
            String tsStr = rs.getString("last_checkin_at");
            if (tsStr != null && !tsStr.isEmpty()) {
                // Truncate nanoseconds to milliseconds for OffsetDateTime parsing
                String cleaned = tsStr.replaceAll("(\\.\\d{3})\\d*", "$1");
                it.setLastCheckinAt(java.time.OffsetDateTime.parse(cleaned).toLocalDateTime());
            }
            return it;
        };
    }

    public List<LeaderboardItem> findTopByTotalPoints(int limit) {
        String sql = "SELECT u.id, u.username, u.total_points, " +
                "(SELECT MAX(checked_at) FROM user_daily_status uds WHERE uds.user_id = u.id) AS last_checkin_at " +
                "FROM users u " +
                "ORDER BY u.total_points DESC, last_checkin_at DESC " +
                "LIMIT ?";
        return jdbcTemplate.query(sql, leaderboardRowMapper(), limit);
    }

    public List<LeaderboardItem> findTopByTotalPoints(int limit, int offset) {
        String sql = "SELECT u.id, u.username, u.total_points, " +
                "(SELECT MAX(checked_at) FROM user_daily_status uds WHERE uds.user_id = u.id) AS last_checkin_at " +
                "FROM users u " +
                "ORDER BY u.total_points DESC, last_checkin_at DESC " +
                "LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, leaderboardRowMapper(), limit, offset);
    }

    private UserRole parseRole(String roleText) {
        if (roleText == null || roleText.isBlank()) {
            return null;
        }
        return UserRole.valueOf(roleText);
    }

    private Boolean parseOnline(Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        if (rawValue instanceof Boolean value) {
            return value;
        }
        if (rawValue instanceof Number value) {
            return value.intValue() != 0;
        }
        return Boolean.parseBoolean(rawValue.toString());
    }

    public void updateRole(Long userId, String newRole) {
        jdbcTemplate.update("UPDATE users SET role = ? WHERE id = ?", newRole, userId);
    }

    public void insertRoleChangeLog(Long targetUserId, Long changedBy, String fromRole, String toRole) {
        jdbcTemplate.update(
                "INSERT INTO role_change_log (target_user_id, changed_by, from_role, to_role, changed_at) VALUES (?, ?, ?, ?, ?)",
                targetUserId, changedBy, fromRole, toRole, java.time.LocalDateTime.now().toString()
        );
    }

    private Long parseLongValue(Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        if (rawValue instanceof Number value) {
            return value.longValue();
        }
        return Long.parseLong(rawValue.toString());
    }
}
