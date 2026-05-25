package com.whut.training.common;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 系统时间提供者（生产环境默认）。
 *
 * <p>直接委托给 {@link LocalDate#now()} / {@link Instant#now()}，不做任何偏移。
 */
@Component
@Profile("!dev")
public class SystemTimeProvider implements TimeProvider {

    @Override
    public LocalDate today() {
        return LocalDate.now();
    }

    @Override
    public LocalDateTime now() {
        return LocalDateTime.now();
    }

    @Override
    public long nowEpochSecond() {
        return Instant.now().getEpochSecond();
    }
}
