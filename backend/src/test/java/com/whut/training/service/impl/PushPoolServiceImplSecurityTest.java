package com.whut.training.service.impl;

import com.whut.training.common.TimeProvider;
import com.whut.training.domain.entity.PushPoolItem;
import com.whut.training.domain.entity.User;
import com.whut.training.domain.enums.UserRole;
import com.whut.training.exception.BusinessException;
import com.whut.training.repository.PushPoolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushPoolServiceImplSecurityTest {

    @Mock
    private PushPoolRepository repository;
    @Mock
    private TimeProvider timeProvider;

    private PushPoolServiceImpl service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new PushPoolServiceImpl(repository, timeProvider, false);
        user = new User(1L, "member", null, "hash", UserRole.USER);
    }

    @Test
    void rejectsScriptSchemeInSubmittedProblem() {
        BusinessException error = assertThrows(
                BusinessException.class,
                () -> service.submit(user, "unsafe", "javascript:alert(1)", null)
        );

        assertEquals(400, error.getCode());
        verify(repository, never()).insert(any(), any(), any(), any());
    }

    @Test
    void rejectsScriptSchemeInSolutionLink() {
        PushPoolItem item = new PushPoolItem(
                3L, "problem", "https://example.com/problem", null, 1L,
                "member", "PUBLISHED", 1, LocalDateTime.now(), null, null
        );
        when(repository.findById(3L)).thenReturn(Optional.of(item));

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> service.submitSolution(user, 3L, "data:text/html,unsafe", null)
        );

        assertEquals(400, error.getCode());
        verify(repository, never()).insertSubmission(any(), any(), any(), any());
    }
}
