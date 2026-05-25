package com.whut.training.common;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 时间提供者接口。
 *
 * <p>将时间获取抽象出来，便于开发环境注入可调节的假时间以测试定时任务与日期相关逻辑。
 * 生产环境使用 {@link SystemTimeProvider}，开发环境使用 {@link AdjustableTimeProvider}。
 */
public interface TimeProvider {

    /** 获取当前日期 */
    LocalDate today();

    /** 获取当前日期时间 */
    LocalDateTime now();

    /** 获取当前 Unix 时间戳（秒） */
    long nowEpochSecond();
}
