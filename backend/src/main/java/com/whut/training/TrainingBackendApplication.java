package com.whut.training;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 后端应用启动入口。
 *
 * <p>该类负责装配 Spring Boot 应用上下文，并开启定时任务支持，供每日题生成与题库同步等后台任务使用。
 */
@SpringBootApplication
@EnableScheduling
public class TrainingBackendApplication {

    /**
     * 启动 Spring Boot 应用。
     *
     * @param args 命令行参数，原样传递给 Spring Boot 启动器。
     */
    public static void main(String[] args) {
        SpringApplication.run(TrainingBackendApplication.class, args);
    }
}
