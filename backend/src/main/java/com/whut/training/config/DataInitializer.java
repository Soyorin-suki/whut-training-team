package com.whut.training.config;

import com.whut.training.domain.entity.User;
import com.whut.training.domain.enums.UserRole;
import com.whut.training.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer {

    private final UserRepository userRepository;

    public DataInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void initAdmin() {
        if (userRepository.existsByUsername("admin")) {
            return;
        }
        userRepository.save(new User(
                null,
                "admin",
                "admin@example.com",
                "admin123",
                UserRole.ADMIN
        ));
    }
}
