package com.whut.training.config;

import com.whut.training.domain.entity.User;
import com.whut.training.domain.enums.UserRole;
import com.whut.training.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 默认数据初始化器。
 *
 * <p>启动时仅创建 SUPER_ADMIN 账号（从配置文件读取账密）。
 * 普通 ADMIN 账号需用户自行注册后由超管提升权限。
 */
@Component
@DependsOn("mySqlInitializer")
public class DataInitializer {

    private final UserRepository userRepository;
    private final String superAdminUsername;
    private final String superAdminPassword;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           @Value("${superAdmin.username:superadmin}") String superAdminUsername,
                           @Value("${superAdmin.password:superadmin123}") String superAdminPassword,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.superAdminUsername = superAdminUsername;
        this.superAdminPassword = superAdminPassword;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void initSuperAdmin() {
        if (!userRepository.existsByUsername(superAdminUsername)) {
            userRepository.save(new User(
                    null,
                    superAdminUsername,
                    superAdminUsername + "@whut.local",
                    passwordEncoder.encode(superAdminPassword),
                    UserRole.SUPER_ADMIN
            ));
        }
    }
}
