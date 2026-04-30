package com.whut.training.service;

import com.whut.training.domain.entity.UserRank;

import java.util.List;

public interface RankService {
    List<UserRank> queryAllRank(Integer page, Integer pageSize);
}
