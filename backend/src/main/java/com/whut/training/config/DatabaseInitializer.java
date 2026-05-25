package com.whut.training.config;

import java.io.IOException;

/**
 * 数据库初始化器接口。
 *
 * <p>应用启动时由 Spring 调用，负责创建数据库结构。
 * 不同数据库有不同的实现（如 SQLite 用 PRAGMA 补列，MySQL 用 information_schema 检查）。
 * 实现类通过 {@code @ConditionalOnProperty} 按 {@code app.database.type} 自动选择。
 */
public interface DatabaseInitializer {

    /**
     * 初始化数据库，包括创建目录（如适用）、建表、补列等。
     *
     * @throws IOException 目录创建失败时抛出
     */
    void init() throws IOException;
}
