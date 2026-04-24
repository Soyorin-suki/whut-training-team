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
            UserRole.valueOf(rs.getString("role"))
    );

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public User save(User user) {
        if (user.getId() == null) {
            jdbcTemplate.update(
                    "INSERT INTO users (username, email, password, role) VALUES (?, ?, ?, ?)",
                    user.getUsername(),
                    user.getEmail(),
                    user.getPassword(),
                    user.getRole().name()
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
                "UPDATE users SET username = ?, email = ?, password = ?, role = ? WHERE id = ?",
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                user.getRole().name(),
                user.getId()
        );
        return user;
    }

    public List<User> findAll() {
        return jdbcTemplate.query("SELECT id, username, email, password, role FROM users", userRowMapper);
    }

    public Optional<User> findById(Long id) {
        List<User> users = jdbcTemplate.query(
                "SELECT id, username, email, password, role FROM users WHERE id = ?",
                userRowMapper,
                id
        );
        return users.stream().findFirst();
    }

    public Optional<User> findByUsername(String username) {
        List<User> users = jdbcTemplate.query(
                "SELECT id, username, email, password, role FROM users WHERE username = ?",
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
}
