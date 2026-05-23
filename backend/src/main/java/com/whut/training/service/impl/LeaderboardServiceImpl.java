package com.whut.training.service.impl;

import com.whut.training.domain.dto.LeaderboardItem;
import com.whut.training.repository.UserRepository;
import com.whut.training.service.LeaderboardService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LeaderboardServiceImpl implements LeaderboardService {

    private final UserRepository userRepository;

    public LeaderboardServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<LeaderboardItem> getTop(int limit, int offset) {
        if (limit <= 0) limit = 10;
        return userRepository.findTopByTotalPoints(limit, offset);
    }

    @Override
    public int countTotal() {
        return userRepository.countAll();
    }
}
