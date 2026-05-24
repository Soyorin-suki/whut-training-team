package com.whut.training.domain.dto;

import java.time.LocalDateTime;

public class LeaderboardItem {
    private Long userId;
    private String username;
    private Integer totalPoints;
    private LocalDateTime lastCheckinAt;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(Integer totalPoints) {
        this.totalPoints = totalPoints;
    }

    public LocalDateTime getLastCheckinAt() {
        return lastCheckinAt;
    }

    public void setLastCheckinAt(LocalDateTime lastCheckinAt) {
        this.lastCheckinAt = lastCheckinAt;
    }
}
