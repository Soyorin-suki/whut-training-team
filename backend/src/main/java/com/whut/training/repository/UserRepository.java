package com.whut.training.repository;

import com.whut.training.domain.entity.User;
import com.whut.training.domain.enums.UserRole;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<User> userRowMapper = (rs, rowNum) -> new User(
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

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public User save(User user) {
        if (user.getId() == null) {
            jdbcTemplate.update(
                    "INSERT INTO users (username, email, password, role, uid, codeforces_rating, max_rating, is_online, last_online_time_seconds, avatar_url) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    user.getUsername(),
                    user.getEmail(),
                    user.getPassword(),
                    user.getRole() == null ? null : user.getRole().name(),
                    user.getUid(),
                    user.getCodeforcesRating(),
                    user.getMaxRating(),
                    user.getOnline(),
                    user.getLastOnlineTimeSeconds(),
                    user.getAvatarUrl()
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
                "UPDATE users SET username = ?, email = ?, password = ?, role = ?, uid = ?, codeforces_rating = ?, max_rating = ?, is_online = ?, last_online_time_seconds = ?, avatar_url = ? WHERE id = ?",
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
                user.getId()
        );
        return user;
    }

    public List<User> findAll() {
        return jdbcTemplate.query("SELECT id, username, email, password, role, uid, codeforces_rating, max_rating, is_online, last_online_time_seconds, avatar_url FROM users", userRowMapper);
    }

    public Optional<User> findById(Long id) {
        List<User> users = jdbcTemplate.query(
                "SELECT id, username, email, password, role, uid, codeforces_rating, max_rating, is_online, last_online_time_seconds, avatar_url FROM users WHERE id = ?",
                userRowMapper,
                id
        );
        return users.stream().findFirst();
    }

    public Optional<User> findByUsername(String username) {
        List<User> users = jdbcTemplate.query(
                "SELECT id, username, email, password, role, uid, codeforces_rating, max_rating, is_online, last_online_time_seconds, avatar_url FROM users WHERE username = ?",
                userRowMapper,
                username
        );
        return users.stream().findFirst();
    }

    public boolean existsByUsername(String username) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM users WHERE username = ?",
                Integer.class,
                username
        );
        return count != null && count > 0;
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
