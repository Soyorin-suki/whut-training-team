package com.whut.training.service;

import com.whut.training.domain.dto.ProblemCommentRequest;
import com.whut.training.domain.entity.ProblemComment;
import com.whut.training.domain.entity.User;
import com.whut.training.domain.enums.UserRole;
import com.whut.training.exception.BusinessException;
import com.whut.training.repository.DailyProblemRepository;
import com.whut.training.repository.ProblemCommentRepository;
import com.whut.training.repository.UserRepository;
import com.whut.training.service.impl.ProblemCommentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProblemCommentServiceImplTest {

    @Mock
    private ProblemCommentRepository problemCommentRepository;

    @Mock
    private DailyProblemRepository dailyProblemRepository;

    @Mock
    private UserRepository userRepository;

    private ProblemCommentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProblemCommentServiceImpl(
                problemCommentRepository,
                dailyProblemRepository,
                userRepository
        );
    }

    @Test
    void getCommentsBuildsTwoLevelRepliesFromReplyChain() {
        var root = new ProblemCommentRepository.ProblemCommentRecord(
                1L, "2000-A", 11L, null, "Root", "2026-05-17T09:00:00+08:00", "alice", null
        );
        var replyToRoot = new ProblemCommentRepository.ProblemCommentRecord(
                2L, "2000-A", 12L, 1L, "Reply root", "2026-05-17T09:05:00+08:00", "bob", null
        );
        var replyToReply = new ProblemCommentRepository.ProblemCommentRecord(
                3L, "2000-A", 13L, 2L, "Reply reply", "2026-05-17T09:10:00+08:00", "carol", null
        );
        var newerRoot = new ProblemCommentRepository.ProblemCommentRecord(
                4L, "2000-A", 14L, null, "New root", "2026-05-17T10:00:00+08:00", "dave", null
        );

        when(dailyProblemRepository.existsProblemKey("2000-A")).thenReturn(true);
        when(problemCommentRepository.findCommentsByProblemKey("2000-A"))
                .thenReturn(List.of(root, replyToRoot, replyToReply, newerRoot));

        var items = service.getComments("2000-A");

        assertEquals(2, items.size());
        assertEquals(4L, items.get(0).id());
        assertEquals(1L, items.get(1).id());
        assertEquals(2, items.get(1).replies().size());
        assertEquals(2L, items.get(1).replies().get(0).id());
        assertEquals("alice", items.get(1).replies().get(0).replyToUsername());
        assertEquals(3L, items.get(1).replies().get(1).id());
        assertEquals("bob", items.get(1).replies().get(1).replyToUsername());
    }

    @Test
    void createCommentRejectsBlankContent() {
        User user = user(10L, "alice");

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.createComment(user, new ProblemCommentRequest("2000-A", "   ", null))
        );

        assertEquals(400, ex.getCode());
        verify(problemCommentRepository, never()).insertComment(anyString(), anyLong(), any(), anyString());
    }

    @Test
    void createCommentRejectsUnknownProblemKey() {
        User user = user(10L, "alice");
        when(dailyProblemRepository.existsProblemKey("2999-Z")).thenReturn(false);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.createComment(user, new ProblemCommentRequest("2999-Z", "Hello", null))
        );

        assertEquals(404, ex.getCode());
    }

    @Test
    void createCommentRejectsUnknownReplyTarget() {
        User user = user(10L, "alice");
        when(dailyProblemRepository.existsProblemKey("2000-A")).thenReturn(true);
        when(problemCommentRepository.findCommentById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.createComment(user, new ProblemCommentRequest("2000-A", "Reply", 99L))
        );

        assertEquals(404, ex.getCode());
    }

    @Test
    void createCommentRejectsReplyToOtherProblem() {
        User user = user(10L, "alice");
        when(dailyProblemRepository.existsProblemKey("2000-A")).thenReturn(true);
        when(problemCommentRepository.findCommentById(20L)).thenReturn(Optional.of(
                new ProblemComment(20L, "2000-B", 11L, null, "Old", "2026-05-17T09:00:00+08:00", null)
        ));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.createComment(user, new ProblemCommentRequest("2000-A", "Reply", 20L))
        );

        assertEquals(409, ex.getCode());
    }

    @Test
    void createCommentReturnsReplyTargetUsername() {
        User user = user(10L, "alice");
        User targetUser = user(11L, "bob");
        ProblemComment target = new ProblemComment(
                20L, "2000-A", 11L, null, "Root", "2026-05-17T09:00:00+08:00", null
        );
        ProblemComment created = new ProblemComment(
                21L, "2000-A", user.getId(), 20L, "Reply", "2026-05-17T09:05:00+08:00", null
        );

        when(dailyProblemRepository.existsProblemKey("2000-A")).thenReturn(true);
        when(problemCommentRepository.findCommentById(20L)).thenReturn(Optional.of(target));
        when(userRepository.findById(11L)).thenReturn(Optional.of(targetUser));
        when(problemCommentRepository.insertComment("2000-A", user.getId(), 20L, "Reply")).thenReturn(created);

        var item = service.createComment(user, new ProblemCommentRequest("2000-A", "Reply", 20L));

        assertEquals(21L, item.id());
        assertEquals(20L, item.replyCommentId());
        assertEquals("bob", item.replyToUsername());
        assertEquals("alice", item.author().username());
    }

    private User user(Long id, String username) {
        User user = new User(id, username, username + "@example.com", "password123", UserRole.USER);
        user.setAvatarUrl("https://example.com/" + username + ".png");
        return user;
    }
}
