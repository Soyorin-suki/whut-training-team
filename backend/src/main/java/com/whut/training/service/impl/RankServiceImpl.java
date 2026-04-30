package com.whut.training.service.impl;

import com.whut.training.aspect.annotation.ServiceLog;
import com.whut.training.domain.entity.UserRank;
import com.whut.training.repository.UserRepository;
import com.whut.training.service.RankService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@ServiceLog
public class RankServiceImpl implements RankService {

    private final UserRepository userRepository;
    public RankServiceImpl(UserRepository userRepository){
        this.userRepository = userRepository;
    }
    @Override
    public ArrayList<UserRank> queryAllRank(Integer page, Integer pageSize) {
        ArrayList<UserRank> userRanks = userRepository.queryAllRank(page, pageSize);
        return userRanks;
    }
}
