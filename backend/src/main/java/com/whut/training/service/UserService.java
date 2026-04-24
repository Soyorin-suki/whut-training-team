package com.whut.training.service;

import com.whut.training.domain.dto.UserCreateRequest;
import com.whut.training.domain.entity.User;

import java.util.List;

public interface UserService {
    User create(UserCreateRequest request);

    List<User> list();

    User getById(Long id);
}

