package com.whut.training.domain.entity;

public class UserRank {
    private Integer rank;
    private String userName;
    private Integer score;

    public UserRank() {
    }

    public UserRank(Integer rank, String userName, Integer score) {
        this.rank = rank;
        this.userName = userName;
        this.score = score;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }
}
