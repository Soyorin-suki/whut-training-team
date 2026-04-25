package com.whut.training.service.impl;

import com.whut.training.aspect.annotation.ServiceLog;
import com.whut.training.domain.dto.AdminCreateUserRequest;
import com.whut.training.domain.dto.UserUpdateRequest;
import com.whut.training.domain.dto.UserRegisterRequest;
import com.whut.training.domain.entity.User;
import com.whut.training.domain.enums.UserRole;
import com.whut.training.exception.BusinessException;
import com.whut.training.repository.UserRepository;
import com.whut.training.service.CodeforcesApiService;
import com.whut.training.service.UserService;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@ServiceLog
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final CodeforcesApiService codeforcesApiService;

    public UserServiceImpl(UserRepository userRepository, CodeforcesApiService codeforcesApiService) {
        this.userRepository = userRepository;
        this.codeforcesApiService = codeforcesApiService;
    }

    @Override
    public User register(UserRegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(400, "username already exists");
        }

        Optional<CodeforcesApiService.CodeforcesUserProfile> profileOptional =
                codeforcesApiService.getUserInfo(request.getUsername());
        if (profileOptional.isEmpty()) {
            throw new BusinessException(400, "username is not a valid Codeforces handle");
        }

        User user = new User(
                null,
                request.getUsername(),
                normalizeNullableText(request.getEmail()),
                request.getPassword(),
                UserRole.USER,
                request.getCodeforcesRating(),
                request.getMaxRating(),
                request.getOnline(),
                request.getLastOnlineTimeSeconds(),
                normalizeNullableText(request.getAvatarUrl())
        );
        enrichFromCodeforcesIfNeeded(user, profileOptional.get());
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

    @Override
    public User updateProfile(Long userId, UserUpdateRequest request) {
        User user = getById(userId);
        if (request == null) {
            return user;
        }

        if (request.username() != null) {
            String nextUsername = request.username().trim();
            if (nextUsername.isEmpty()) {
                throw new BusinessException(400, "username cannot be empty");
            }
            if (!nextUsername.equals(user.getUsername())) {
                if (userRepository.existsByUsername(nextUsername)) {
                    throw new BusinessException(400, "username already exists");
                }
                Optional<CodeforcesApiService.CodeforcesUserProfile> profileOptional =
                        codeforcesApiService.getUserInfo(nextUsername);
                if (profileOptional.isEmpty()) {
                    throw new BusinessException(400, "username is not a valid Codeforces handle");
                }
                user.setUsername(nextUsername);
                syncCodeforcesStats(user, profileOptional.get());
            }
        }

        if (request.email() != null) {
            user.setEmail(normalizeNullableText(request.email()));
        }

        if (request.password() != null) {
            String password = request.password().trim();
            if (!password.isEmpty()) {
                if (password.length() < 6) {
                    throw new BusinessException(400, "password length must be at least 6");
                }
                user.setPassword(password);
            }
        }
        return userRepository.save(user);
    }

    private void syncCodeforcesStats(User user, CodeforcesApiService.CodeforcesUserProfile profile) {
        user.setCodeforcesRating(profile.rating());
        user.setMaxRating(profile.maxRating());
        user.setOnline(profile.online());
        user.setLastOnlineTimeSeconds(profile.lastOnlineTimeSeconds());
    }

    private void enrichFromCodeforcesIfNeeded(User user, CodeforcesApiService.CodeforcesUserProfile profile) {
        boolean needEnrich = user.getCodeforcesRating() == null
                || user.getMaxRating() == null
                || user.getOnline() == null
                || user.getLastOnlineTimeSeconds() == null
                || user.getAvatarUrl() == null
                || user.getAvatarUrl().isBlank();
        if (!needEnrich) {
            user.setUid(parseUidFromAvatarUrl(user.getAvatarUrl()));
            return;
        }

        if (user.getCodeforcesRating() == null) {
            user.setCodeforcesRating(profile.rating());
        }
        if (user.getMaxRating() == null) {
            user.setMaxRating(profile.maxRating());
        }
        if (user.getOnline() == null) {
            user.setOnline(profile.online());
        }
        if (user.getLastOnlineTimeSeconds() == null) {
            user.setLastOnlineTimeSeconds(profile.lastOnlineTimeSeconds());
        }
        if (user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) {
            user.setAvatarUrl(profile.avatarUrl());
        }
        user.setUid(parseUidFromAvatarUrl(user.getAvatarUrl()));
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Long parseUidFromAvatarUrl(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(avatarUrl.trim());
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                return null;
            }
            String[] segments = path.split("/");
            for (String segment : segments) {
                if (segment == null || segment.isBlank()) {
                    continue;
                }
                if (segment.chars().allMatch(Character::isDigit)) {
                    return Long.parseLong(segment);
                }
                break;
            }
            return null;
        } catch (Exception ex) {
            return null;
        }
    }
}
