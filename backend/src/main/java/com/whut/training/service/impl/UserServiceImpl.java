package com.whut.training.service.impl;

import com.whut.training.domain.dto.AdminCreateUserRequest;
import com.whut.training.domain.dto.UserRegisterRequest;
import com.whut.training.domain.entity.User;
import com.whut.training.domain.enums.UserRole;
import com.whut.training.exception.BusinessException;
import com.whut.training.repository.UserRepository;
import com.whut.training.service.UserService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User register(UserRegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(400, "username already exists");
        }
        User user = new User(null, request.getUsername(), request.getEmail(), request.getPassword(), UserRole.USER);
        return userRepository.save(user);
    }

    @Override
    public User createByAdmin(AdminCreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(400, "username already exists");
        }
        User user = new User(null, request.getUsername(), request.getEmail(), request.getPassword(), request.getRole());
        return userRepository.save(user);
    }

    @Override
    public List<User> list() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getId))
                .toList();
    }

    @Override
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "user not found: " + id));
    }

    @Override
    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(404, "user not found: " + username));
    }
}
