package com.whut.training.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.whut.training.domain.enums.UserRole;
public class User {
    private Long id;
    private String username;
    private String email;
    @JsonIgnore
    private String password;
    private UserRole role;
    private Long uid;
    private Integer codeforcesRating;
    private Integer maxRating;
    private Boolean online;
    private Long lastOnlineTimeSeconds;
    private String avatarUrl;
    private Integer score;
    private Integer solvedProblemCount;
    private Integer hardSolvedProblemCount;
    private Integer solved800To1400Count;
    private Integer solved1500To2200Count;
    private Integer solvedAbove2200Count;
    private Integer currentStreakDays;
    private Integer longestStreakDays;

    public User() {
    }

    public User(Long id, String username, String email, String password, UserRole role) {
        this(id, username, email, password, role, null, null, null, null, null);
    }

    public User(Long id, String username, String email, String password, UserRole role, Integer codeforcesRating, Integer maxRating) {
        this(id, username, email, password, role, codeforcesRating, maxRating, null, null, null);
    }

    public User(Long id, String username, String email, String password, UserRole role, Integer codeforcesRating, Integer maxRating,
                Boolean online, Long lastOnlineTimeSeconds, String avatarUrl) {
        this(id, username, email, password, role, codeforcesRating, maxRating, online, lastOnlineTimeSeconds, avatarUrl, null);
    }

    public User(Long id, String username, String email, String password, UserRole role, Integer codeforcesRating, Integer maxRating,
                Boolean online, Long lastOnlineTimeSeconds, String avatarUrl, Long uid) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.uid = uid;
        this.codeforcesRating = codeforcesRating;
        this.maxRating = maxRating;
        this.online = online;
        this.lastOnlineTimeSeconds = lastOnlineTimeSeconds;
        this.avatarUrl = avatarUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public Long getUid() {
        return uid;
    }

    public void setUid(Long uid) {
        this.uid = uid;
    }

    public Integer getCodeforcesRating() {
        return codeforcesRating;
    }

    public void setCodeforcesRating(Integer codeforcesRating) {
        this.codeforcesRating = codeforcesRating;
    }

    public Integer getMaxRating() {
        return maxRating;
    }

    public void setMaxRating(Integer maxRating) {
        this.maxRating = maxRating;
    }

    public Boolean getOnline() {
        return online;
    }

    public void setOnline(Boolean online) {
        this.online = online;
    }

    public Long getLastOnlineTimeSeconds() {
        return lastOnlineTimeSeconds;
    }

    public void setLastOnlineTimeSeconds(Long lastOnlineTimeSeconds) {
        this.lastOnlineTimeSeconds = lastOnlineTimeSeconds;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getSolvedProblemCount() {
        return solvedProblemCount;
    }

    public void setSolvedProblemCount(Integer solvedProblemCount) {
        this.solvedProblemCount = solvedProblemCount;
    }

    public Integer getHardSolvedProblemCount() {
        return hardSolvedProblemCount;
    }

    public void setHardSolvedProblemCount(Integer hardSolvedProblemCount) {
        this.hardSolvedProblemCount = hardSolvedProblemCount;
    }

    public Integer getCurrentStreakDays() {
        return currentStreakDays;
    }

    public Integer getSolved800To1400Count() {
        return solved800To1400Count;
    }

    public void setSolved800To1400Count(Integer solved800To1400Count) {
        this.solved800To1400Count = solved800To1400Count;
    }

    public Integer getSolved1500To2200Count() {
        return solved1500To2200Count;
    }

    public void setSolved1500To2200Count(Integer solved1500To2200Count) {
        this.solved1500To2200Count = solved1500To2200Count;
    }

    public Integer getSolvedAbove2200Count() {
        return solvedAbove2200Count;
    }

    public void setSolvedAbove2200Count(Integer solvedAbove2200Count) {
        this.solvedAbove2200Count = solvedAbove2200Count;
    }

    public void setCurrentStreakDays(Integer currentStreakDays) {
        this.currentStreakDays = currentStreakDays;
    }

    public Integer getLongestStreakDays() {
        return longestStreakDays;
    }

    public void setLongestStreakDays(Integer longestStreakDays) {
        this.longestStreakDays = longestStreakDays;
    }
}
