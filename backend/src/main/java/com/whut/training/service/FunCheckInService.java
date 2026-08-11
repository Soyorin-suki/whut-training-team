package com.whut.training.service;

import com.whut.training.common.TimeProvider;
import com.whut.training.domain.dto.FunCheckInItem;
import com.whut.training.repository.FunCheckInRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 每日趣味签到与运势抽取。
 *
 * <p>运势按用户和日期稳定生成，同一天重复请求会返回同一结果，避免刷新页面后变化。
 */
@Service
public class FunCheckInService {

    private final FunCheckInRepository repository;
    private final TimeProvider timeProvider;
    private final FortuneCatalog fortuneCatalog;

    public FunCheckInService(
            FunCheckInRepository repository,
            TimeProvider timeProvider,
            FortuneCatalog fortuneCatalog
    ) {
        this.repository = repository;
        this.timeProvider = timeProvider;
        this.fortuneCatalog = fortuneCatalog;
    }

    public FunCheckInItem checkIn(Long userId) {
        LocalDate today = timeProvider.today();
        return repository.findByUserAndDate(userId, today).orElseGet(() -> {
            FortuneCatalog.Fortune fortune = fortuneCatalog.select(userId, today);
            FunCheckInItem candidate = new FunCheckInItem(
                    null,
                    userId,
                    today.toString(),
                    fortune.key(),
                    fortune.title(),
                    fortune.message(),
                    fortune.luckyTag(),
                    fortune.color(),
                    fortune.level(),
                    timeProvider.now().toString()
            );
            repository.insert(candidate);
            return repository.findByUserAndDate(userId, today).orElse(candidate);
        });
    }

    public List<FunCheckInItem> history(Long userId, int days) {
        LocalDate endDate = timeProvider.today();
        LocalDate startDate = endDate.minusDays(Math.max(1, days) - 1L);
        return repository.findRange(userId, startDate, endDate);
    }

}
