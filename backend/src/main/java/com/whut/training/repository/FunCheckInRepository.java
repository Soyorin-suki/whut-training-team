package com.whut.training.repository;

import com.whut.training.domain.dto.FunCheckInItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 趣味签到持久化。数据库唯一键保证每名用户每天只会抽取一次运势。
 */
@Repository
public class FunCheckInRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<FunCheckInItem> rowMapper = (rs, rowNum) -> new FunCheckInItem(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getString("check_in_date"),
            rs.getString("fortune_key"),
            rs.getString("fortune_title"),
            rs.getString("fortune_message"),
            rs.getString("lucky_tag"),
            rs.getString("lucky_color"),
            rs.getInt("luck_level"),
            rs.getString("checked_at")
    );

    public FunCheckInRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<FunCheckInItem> findByUserAndDate(Long userId, LocalDate date) {
        List<FunCheckInItem> rows = jdbcTemplate.query(
                "SELECT * FROM user_fun_check_in WHERE user_id = ? AND check_in_date = ?",
                rowMapper,
                userId,
                date.toString()
        );
        return rows.stream().findFirst();
    }

    public List<FunCheckInItem> findRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return jdbcTemplate.query(
                "SELECT * FROM user_fun_check_in WHERE user_id = ? AND check_in_date BETWEEN ? AND ? ORDER BY check_in_date ASC",
                rowMapper,
                userId,
                startDate.toString(),
                endDate.toString()
        );
    }

    public void insert(FunCheckInItem item) {
        jdbcTemplate.update(
                """
                INSERT IGNORE INTO user_fun_check_in (
                    user_id, check_in_date, fortune_key, fortune_title, fortune_message,
                    lucky_tag, lucky_color, luck_level, checked_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                item.userId(), item.date(), item.fortuneKey(), item.fortuneTitle(),
                item.fortuneMessage(), item.luckyTag(), item.luckyColor(),
                item.luckLevel(), item.checkedAt()
        );
    }
}
