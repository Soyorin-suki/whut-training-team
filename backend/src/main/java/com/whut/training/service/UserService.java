package com.whut.training.service;

import com.whut.training.domain.dto.AdminCreateUserRequest;
import com.whut.training.domain.dto.UserRegisterRequest;
import com.whut.training.domain.entity.User;

import java.util.List;

public interface UserService {
    User register(UserRegisterRequest request);

    User createByAdmin(AdminCreateUserRequest request);

    List<User> list();

    User getById(Long id);

    User getByUsername(String username);
}
