package com.whut.training.service;

import com.whut.training.domain.dto.DailyProblemCommentRequest;
import com.whut.training.domain.entity.DailyProblemComment;
import com.whut.training.domain.entity.User;
import com.whut.training.domain.enums.UserRole;
import com.whut.training.exception.BusinessException;
import com.whut.training.repository.DailyProblemCommentRepository;
import com.whut.training.repository.DailyProblemRepository;
import com.whut.training.repository.UserRepository;
import com.whut.training.service.impl.DailyProblemCommentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyProblemCommentServiceImplTest {

    @Mock
    private DailyProblemCommentRepository dailyProblemCommentRepository;

    @Mock
    private DailyProblemRepository dailyProblemRepository;

    @Mock
    private UserRepository userRepository;

    private DailyProblemCommentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DailyProblemCommentServiceImpl(
                dailyProblemCommentRepository,
                dailyProblemRepository,
                userRepository
        );
    }

    @Test
    void getTodayCommentsBuildsTwoLevelRepliesFromReplyChain() {
        LocalDate today = LocalDate.now();
        var root = new DailyProblemCommentRepository.DailyProblemCommentRecord(
                1L, today, "2000-A", 11L, null, "Root", "2026-05-17T09:00:00+08:00", "alice", null
        );
        var replyToRoot = new DailyProblemCommentRepository.DailyProblemCommentRecord(
                2L, today, "2000-A", 12L, 1L, "Reply root", "2026-05-17T09:05:00+08:00", "bob", null
        );
        var replyToReply = new DailyProblemCommentRepository.DailyProblemCommentRecord(
                3L, today, "2000-A", 13L, 2L, "Reply reply", "2026-05-17T09:10:00+08:00", "carol", null
        );
        var newerRoot = new DailyProblemCommentRepository.DailyProblemCommentRecord(
                4L, today, "2000-A", 14L, null, "New root", "2026-05-17T10:00:00+08:00", "dave", null
        );

        when(dailyProblemCommentRepository.findCommentsByDailyProblem(today, "2000-A"))
                .thenReturn(List.of(root, replyToRoot, replyToReply, newerRoot));

        var items = service.getComments(today, "2000-A");

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
                () -> service.createComment(user, LocalDate.now(), "2000-A", new DailyProblemCommentRequest("   ", null))
        );

        assertEquals(400, ex.getCode());
        verify(dailyProblemCommentRepository, never()).insertComment(any(), anyString(), anyLong(), any(), anyString());
    }

    @Test
    void createCommentRejectsUnknownReplyTarget() {
        User user = user(10L, "alice");
        LocalDate today = LocalDate.now();

        when(dailyProblemCommentRepository.findCommentById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.createComment(user, today, "2000-A", new DailyProblemCommentRequest("Reply", 99L))
        );

        assertEquals(404, ex.getCode());
    }

    @Test
    void createCommentRejectsUnknownRootInstance() {
        User user = user(10L, "alice");
        LocalDate targetDate = LocalDate.now().minusDays(7);

        when(dailyProblemCommentRepository.existsCommentsByDailyProblem(targetDate, "1999-B")).thenReturn(false);
        when(dailyProblemRepository.existsDailyProblemInstance(targetDate, "1999-B")).thenReturn(false);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.createComment(user, targetDate, "1999-B", new DailyProblemCommentRequest("Root", null))
        );

        assertEquals(404, ex.getCode());
        verify(dailyProblemCommentRepository, never()).insertComment(any(), anyString(), anyLong(), any(), anyString());
    }

    @Test
    void createCommentRejectsReplyToOtherProblemInstance() {
        User user = user(10L, "alice");
        LocalDate today = LocalDate.now();

        when(dailyProblemCommentRepository.findCommentById(20L)).thenReturn(Optional.of(
                new DailyProblemComment(20L, today.minusDays(1), "2000-A", 11L, null, "Old", "2026-05-16T09:00:00+08:00")
        ));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.createComment(user, today, "2000-A", new DailyProblemCommentRequest("Reply", 20L))
        );

        assertEquals(409, ex.getCode());
    }

    @Test
    void createCommentAllowsHistoricalRootWhenInstanceExists() {
        User user = user(10L, "alice");
        LocalDate historicalDate = LocalDate.now().minusDays(2);
        DailyProblemComment created = new DailyProblemComment(
                21L, historicalDate, "1999-B", user.getId(), null, "Root", "2026-05-15T09:05:00+08:00"
        );

        when(dailyProblemCommentRepository.existsCommentsByDailyProblem(historicalDate, "1999-B")).thenReturn(false);
        when(dailyProblemRepository.existsDailyProblemInstance(historicalDate, "1999-B")).thenReturn(true);
        when(dailyProblemCommentRepository.insertComment(historicalDate, "1999-B", user.getId(), null, "Root"))
                .thenReturn(created);

        var item = service.createComment(user, historicalDate, "1999-B", new DailyProblemCommentRequest("Root", null));

        assertEquals(21L, item.id());
        assertEquals(historicalDate.toString(), item.dailyProblemDate());
        assertEquals("1999-B", item.problemKey());
        assertEquals("alice", item.author().username());
    }

    @Test
    void createCommentAllowsHistoricalRootWhenArchivedThreadAlreadyExists() {
        User user = user(10L, "alice");
        LocalDate regeneratedDate = LocalDate.now().minusDays(1);
        DailyProblemComment created = new DailyProblemComment(
                22L, regeneratedDate, "1998-C", user.getId(), null, "Follow-up", "2026-05-16T10:05:00+08:00"
        );

        when(dailyProblemCommentRepository.existsCommentsByDailyProblem(regeneratedDate, "1998-C")).thenReturn(true);
        doReturn(created).when(dailyProblemCommentRepository)
                .insertComment(regeneratedDate, "1998-C", user.getId(), null, "Follow-up");

        var item = service.createComment(
                user,
                regeneratedDate,
                "1998-C",
                new DailyProblemCommentRequest("Follow-up", null)
        );

        assertEquals(22L, item.id());
        assertEquals("1998-C", item.problemKey());
        verify(dailyProblemRepository, never()).existsDailyProblemInstance(any(), anyString());
    }

    @Test
    void createCommentReturnsReplyTargetUsername() {
        User user = user(10L, "alice");
        LocalDate today = LocalDate.now();
        DailyProblemComment target = new DailyProblemComment(
                20L, today, "2000-A", 11L, null, "Root", "2026-05-17T09:00:00+08:00"
        );
        User targetUser = user(11L, "bob");
        DailyProblemComment created = new DailyProblemComment(
                21L, today, "2000-A", user.getId(), 20L, "Reply", "2026-05-17T09:05:00+08:00"
        );

        when(dailyProblemCommentRepository.findCommentById(20L)).thenReturn(Optional.of(target));
        when(userRepository.findById(11L)).thenReturn(Optional.of(targetUser));
        when(dailyProblemCommentRepository.insertComment(today, "2000-A", user.getId(), 20L, "Reply"))
                .thenReturn(created);

        var item = service.createComment(user, today, "2000-A", new DailyProblemCommentRequest("Reply", 20L));

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
