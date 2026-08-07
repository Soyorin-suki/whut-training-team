package com.whut.training.service.impl;

import com.whut.training.common.TimeProvider;
import com.whut.training.domain.dto.CodeforcesBindingStartRequest;
import com.whut.training.domain.dto.UserRegisterRequest;
import com.whut.training.domain.entity.User;
import com.whut.training.domain.enums.UserRole;
import com.whut.training.repository.UserRepository;
import com.whut.training.service.CodeforcesApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CodeforcesApiService codeforcesApiService;
    @Mock
    private TimeProvider timeProvider;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, codeforcesApiService, timeProvider);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void registerCreatesSiteAccountWithoutCallingCodeforces() {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername("site-user");
        request.setDisplayName("Site User");
        request.setEmail("site@example.com");
        request.setPassword("123456");
        when(userRepository.existsByUsername("site-user")).thenReturn(false);

        User saved = userService.register(request);

        assertEquals("site-user", saved.getUsername());
        assertEquals("Site User", saved.getDisplayName());
        assertNull(saved.getCodeforcesHandle());
        verify(codeforcesApiService, never()).getUserInfo(any());
    }

    @Test
    void finishBindingStoresHandleAfterOwnershipSubmission() {
        User user = new User(1L, "site-user", null, "123456", UserRole.USER);
        user.setPendingCodeforcesHandle("tourist");
        user.setCodeforcesBindingStartedAtSeconds(1_000L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(timeProvider.nowEpochSecond()).thenReturn(1_050L);
        when(codeforcesApiService.hasOwnershipVerificationSubmission("tourist", 1_000L))
                .thenReturn(Optional.of(true));
        when(userRepository.findByCodeforcesHandle("tourist")).thenReturn(Optional.empty());
        when(codeforcesApiService.getUserInfo("tourist"))
                .thenReturn(Optional.of(new CodeforcesApiService.CodeforcesUserProfile(
                        3800, 4000, true, 1_050L, null
                )));

        User saved = userService.finishCodeforcesBinding(1L);

        assertEquals("tourist", saved.getCodeforcesHandle());
        assertEquals(3800, saved.getCodeforcesRating());
        assertNull(saved.getPendingCodeforcesHandle());
        assertNull(saved.getCodeforcesBindingStartedAtSeconds());
    }

    @Test
    void startBindingStoresPendingVerificationWindow() {
        User user = new User(1L, "site-user", null, "123456", UserRole.USER);
        CodeforcesBindingStartRequest request = new CodeforcesBindingStartRequest();
        request.setHandle(" tourist ");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByCodeforcesHandle("tourist")).thenReturn(Optional.empty());
        when(codeforcesApiService.getUserInfo("tourist"))
                .thenReturn(Optional.of(new CodeforcesApiService.CodeforcesUserProfile(
                        null, null, false, null, null
                )));
        when(timeProvider.nowEpochSecond()).thenReturn(2_000L);

        var response = userService.startCodeforcesBinding(1L, request);

        assertEquals("tourist", user.getPendingCodeforcesHandle());
        assertEquals(2_000L, user.getCodeforcesBindingStartedAtSeconds());
        assertEquals(2_120L, response.expiresAtSeconds());
    }
}
