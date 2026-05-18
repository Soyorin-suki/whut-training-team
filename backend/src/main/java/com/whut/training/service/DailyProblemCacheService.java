package com.whut.training.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whut.training.domain.entity.DailyProblem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;

@Component
public class DailyProblemCacheService {

    private static final Logger log = LoggerFactory.getLogger(DailyProblemCacheService.class);
    private static final String KEY_PREFIX = "daily_problem:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration cacheTtl;

    public DailyProblemCacheService(
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper,
            @Value("${app.daily-problem.cache-ttl:1d}") Duration cacheTtl) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.cacheTtl = cacheTtl;
    }

    public Optional<DailyProblem> get(LocalDate date) {
        try {
            String payload = stringRedisTemplate.opsForValue().get(buildKey(date));
            if (payload == null || payload.isBlank()) {
                return Optional.empty();
            }
            CachedDailyProblem cached = objectMapper.readValue(payload, CachedDailyProblem.class);
            return Optional.of(cached.toDailyProblem());
        } catch (Exception ex) {
            log.warn("daily problem cache read failed for date={}", date, ex);
            return Optional.empty();
        }
    }

    public void put(DailyProblem dailyProblem) {
        try {
            String payload = objectMapper.writeValueAsString(CachedDailyProblem.from(dailyProblem));
            stringRedisTemplate.opsForValue().set(buildKey(dailyProblem.date()), payload, cacheTtl);
        } catch (JsonProcessingException ex) {
            log.warn("daily problem cache serialization failed for date={}", dailyProblem.date(), ex);
        } catch (Exception ex) {
            log.warn("daily problem cache write failed for date={}", dailyProblem.date(), ex);
        }
    }

    public void evict(LocalDate date) {
        try {
            stringRedisTemplate.delete(buildKey(date));
        } catch (Exception ex) {
            log.warn("daily problem cache eviction failed for date={}", date, ex);
        }
    }

    private String buildKey(LocalDate date) {
        return KEY_PREFIX + date;
    }

    private record CachedDailyProblem(
            Long id,
            String date,
            String problemKey,
            Integer contestId,
            String problemIndex,
            String name,
            Integer rating,
            String tags,
            String sourceUrl
    ) {
        private static CachedDailyProblem from(DailyProblem dailyProblem) {
            return new CachedDailyProblem(
                    dailyProblem.id(),
                    dailyProblem.date().toString(),
                    dailyProblem.problemKey(),
                    dailyProblem.contestId(),
                    dailyProblem.problemIndex(),
                    dailyProblem.name(),
                    dailyProblem.rating(),
                    dailyProblem.tags(),
                    dailyProblem.sourceUrl()
            );
        }

        private DailyProblem toDailyProblem() {
            return new DailyProblem(
                    id,
                    LocalDate.parse(date),
                    problemKey,
                    contestId,
                    problemIndex,
                    name,
                    rating,
                    tags,
                    sourceUrl
            );
        }
    }
}
