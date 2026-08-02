package com.whut.training.config;

import java.io.IOException;

/**
 * 数据库初始化器接口。
 *
 * <p>应用启动时由 Spring 调用，负责创建数据库结构。
 * 当前运行时使用 MySQL，实现类通过 {@code app.database.type=mysql} 激活。
 */
public interface DatabaseInitializer {

    /**
     * 初始化数据库，包括创建目录（如适用）、建表、补列等。
     *
     * @throws IOException 目录创建失败时抛出
     */
    void init() throws IOException;
}
