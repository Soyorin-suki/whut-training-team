package com.whut.training.config;

import com.whut.training.domain.entity.User;
import com.whut.training.domain.enums.UserRole;
import com.whut.training.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

/**
 * 默认数据初始化器。
 *
 * <p>启动时自动补齐管理员账号。当前实现会写入默认账号和明文密码，属于开发期便捷配置，生产环境应替换为安全初始化方案。
 */
@Component
@DependsOn("sqliteInitializer")
public class DataInitializer {

    private final UserRepository userRepository;

    /**
     * 创建默认数据初始化器。
     *
     * @param userRepository 用户仓储。
     */
    public DataInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 初始化默认管理员与超管理员。
     */
    @PostConstruct
    public void initAdmin() {
        if (!userRepository.existsByUsername("admin")) {
            userRepository.save(new User(
                    null,
                    "admin",
                    "admin@example.com",
                    "admin123",
                    UserRole.ADMIN
            ));
        }
        if (!userRepository.existsByUsername("superadmin")) {
            userRepository.save(new User(
                    null,
                    "superadmin",
                    "superadmin@example.com",
                    "superadmin123",
                    UserRole.SUPER_ADMIN
            ));
        }
    }
}
