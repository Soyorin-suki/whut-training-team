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
}
