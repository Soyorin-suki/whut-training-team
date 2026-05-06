package com.whut.training.service.impl;

import com.whut.training.aspect.annotation.ServiceLog;
import com.whut.training.domain.dto.LeaderboardEntryResponse;
import com.whut.training.domain.dto.LeaderboardPageResponse;
import com.whut.training.domain.enums.LeaderboardType;
import com.whut.training.exception.BusinessException;
import com.whut.training.repository.UserRepository;
import com.whut.training.repository.UserRepository.LeaderboardUserSnapshot;
import com.whut.training.service.RankService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ServiceLog
public class RankServiceImpl implements RankService {

    private final UserRepository userRepository;

    public RankServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public LeaderboardPageResponse getLeaderboard(String type, String page, String pageSize, Long currentUserId) {
        LeaderboardType leaderboardType = parseAndValidateType(type);
        int resolvedPage = parsePositiveInt(page, "page", 1);
        int resolvedPageSize = parsePositiveInt(pageSize, "pageSize", 20);
        if (resolvedPageSize > 100) {
            throw new BusinessException(400, "pageSize must be between 1 and 100");
        }

        long total = userRepository.countLeaderboardEntries();
        int offset = (resolvedPage - 1) * resolvedPageSize;
        List<LeaderboardUserSnapshot> rows = userRepository.findLeaderboardPage(leaderboardType, resolvedPageSize, offset);
        List<LeaderboardEntryResponse> entries = java.util.stream.IntStream.range(0, rows.size())
                .mapToObj(index -> toEntry(rows.get(index), offset + index + 1, currentUserId))
                .toList();

        return new LeaderboardPageResponse(
                leaderboardType,
                resolvedPage,
                resolvedPageSize,
                total,
                entries,
                getMyLeaderboardEntry(type, currentUserId)
        );
    }

    @Override
    public LeaderboardEntryResponse getMyLeaderboardEntry(String type, Long currentUserId) {
        LeaderboardType leaderboardType = parseAndValidateType(type);
        LeaderboardUserSnapshot row = userRepository.findLeaderboardUserById(leaderboardType, currentUserId)
                .orElseThrow(() -> new BusinessException(404, "user not found"));
        int rank = userRepository.countUsersAheadOf(leaderboardType, currentUserId) + 1;
        return new LeaderboardEntryResponse(
                rank,
                row.userId(),
                row.username(),
                row.avatarUrl(),
                row.score(),
                true
        );
    }

    private LeaderboardType parseAndValidateType(String type) {
        LeaderboardType leaderboardType;
        try {
            leaderboardType = LeaderboardType.from(type);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(400, "unsupported leaderboard type");
        }
        if (!leaderboardType.isSupported()) {
            throw new BusinessException(400, "unsupported leaderboard type");
        }
        return leaderboardType;
    }

    private int parsePositiveInt(String rawValue, String fieldName, int defaultValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(rawValue.trim());
            if (parsed < 1) {
                throw new BusinessException(400, fieldName + " must be at least 1");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new BusinessException(400, fieldName + " must be a number");
        }
    }

    private LeaderboardEntryResponse toEntry(LeaderboardUserSnapshot row, int rank, Long currentUserId) {
        return new LeaderboardEntryResponse(
                rank,
                row.userId(),
                row.username(),
                row.avatarUrl(),
                row.score(),
                currentUserId != null && currentUserId.equals(row.userId())
        );
    }
}
