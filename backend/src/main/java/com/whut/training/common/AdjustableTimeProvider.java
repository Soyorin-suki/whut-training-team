package com.whut.training.common;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 可调节时间提供者（仅开发环境）。
 *
 * <p>支持通过 {@link #setFixedTime(LocalDateTime)} 设置一个固定时间点；
 * 之后所有时间查询都基于该固定时间计算。调用 {@link #reset()} 恢复系统时间。
 */
@Component
@Profile("dev")
public class AdjustableTimeProvider implements TimeProvider {

    private volatile LocalDateTime fixedTime;

    /** 设置固定时间点 */
    public void setFixedTime(LocalDateTime time) {
        this.fixedTime = time;
    }

    /** 恢复为系统真实时间 */
    public void reset() {
        this.fixedTime = null;
    }

    /** 获取当前设置的时间（null 表示未设置，使用系统时间） */
    public LocalDateTime getFixedTime() {
        return fixedTime;
    }

    private LocalDateTime effectiveNow() {
        return fixedTime != null ? fixedTime : LocalDateTime.now();
    }

    @Override
    public LocalDate today() {
        return effectiveNow().toLocalDate();
    }

    @Override
    public LocalDateTime now() {
        return effectiveNow();
    }

    @Override
    public long nowEpochSecond() {
        return effectiveNow().atZone(ZoneId.systemDefault()).toEpochSecond();
    }
}
