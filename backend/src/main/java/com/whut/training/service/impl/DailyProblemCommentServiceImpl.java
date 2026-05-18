package com.whut.training.service.impl;

import com.whut.training.aspect.annotation.ServiceLog;
import com.whut.training.domain.dto.DailyProblemCommentArchiveItem;
import com.whut.training.domain.dto.DailyProblemCommentAuthor;
import com.whut.training.domain.dto.DailyProblemCommentItem;
import com.whut.training.domain.dto.DailyProblemCommentRequest;
import com.whut.training.domain.entity.DailyProblemComment;
import com.whut.training.domain.entity.User;
import com.whut.training.exception.BusinessException;
import com.whut.training.repository.DailyProblemCommentRepository;
import com.whut.training.repository.DailyProblemRepository;
import com.whut.training.repository.UserRepository;
import com.whut.training.service.DailyProblemCommentService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@ServiceLog
public class DailyProblemCommentServiceImpl implements DailyProblemCommentService {

    private final DailyProblemCommentRepository dailyProblemCommentRepository;
    private final DailyProblemRepository dailyProblemRepository;
    private final UserRepository userRepository;

    public DailyProblemCommentServiceImpl(DailyProblemCommentRepository dailyProblemCommentRepository,
                                          DailyProblemRepository dailyProblemRepository,
                                          UserRepository userRepository) {
        this.dailyProblemCommentRepository = dailyProblemCommentRepository;
        this.dailyProblemRepository = dailyProblemRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<DailyProblemCommentArchiveItem> getCommentArchives(int limit) {
        return List.copyOf(dailyProblemCommentRepository.findCommentArchives(limit));
    }

    @Override
    public List<DailyProblemCommentItem> getComments(LocalDate dailyProblemDate, String problemKey) {
        LocalDate safeDate = requireDailyProblemDate(dailyProblemDate);
        String safeProblemKey = normalizeProblemKey(problemKey);
        List<DailyProblemCommentRepository.DailyProblemCommentRecord> rows =
                dailyProblemCommentRepository.findCommentsByDailyProblem(safeDate, safeProblemKey);
        if (rows.isEmpty()) {
            return List.of();
        }

        Map<Long, DailyProblemCommentRepository.DailyProblemCommentRecord> commentById = new HashMap<>();
        for (DailyProblemCommentRepository.DailyProblemCommentRecord row : rows) {
            commentById.put(row.id(), row);
        }

        List<DailyProblemCommentRepository.DailyProblemCommentRecord> rootComments = new ArrayList<>();
        Map<Long, List<DailyProblemCommentItem>> repliesByRootId = new HashMap<>();
        for (DailyProblemCommentRepository.DailyProblemCommentRecord row : rows) {
            if (row.replyCommentId() == null) {
                rootComments.add(row);
                continue;
            }

            Long rootCommentId = findRootCommentId(row.id(), commentById);
            if (rootCommentId.equals(row.id())) {
                rootComments.add(row);
                continue;
            }

            repliesByRootId.computeIfAbsent(rootCommentId, ignored -> new ArrayList<>())
                    .add(toCommentItem(row, resolveReplyToUsername(row.replyCommentId(), commentById), List.of()));
        }

        Comparator<DailyProblemCommentRepository.DailyProblemCommentRecord> rootComparator = Comparator
                .comparing(DailyProblemCommentRepository.DailyProblemCommentRecord::createdAt)
                .thenComparing(DailyProblemCommentRepository.DailyProblemCommentRecord::id)
                .reversed();
        Comparator<DailyProblemCommentItem> replyComparator = Comparator
                .comparing(DailyProblemCommentItem::createdAt)
                .thenComparing(DailyProblemCommentItem::id);

        rootComments.sort(rootComparator);
        repliesByRootId.values().forEach(items -> items.sort(replyComparator));

        List<DailyProblemCommentItem> items = new ArrayList<>();
        for (DailyProblemCommentRepository.DailyProblemCommentRecord rootComment : rootComments) {
            items.add(toCommentItem(
                    rootComment,
                    null,
                    List.copyOf(repliesByRootId.getOrDefault(rootComment.id(), List.of()))
            ));
        }
        return List.copyOf(items);
    }

    @Override
    public DailyProblemCommentItem createComment(User user, LocalDate dailyProblemDate, String problemKey,
                                                 DailyProblemCommentRequest request) {
        if (user == null || user.getId() == null) {
            throw new BusinessException(401, "unauthorized");
        }
        LocalDate safeDate = requireDailyProblemDate(dailyProblemDate);
        String safeProblemKey = normalizeProblemKey(problemKey);
        String normalizedContent = normalizeContent(request == null ? null : request.content());
        Long replyCommentId = request == null ? null : request.replyCommentId();

        String replyToUsername = null;
        if (replyCommentId != null) {
            if (replyCommentId <= 0) {
                throw new BusinessException(400, "replyCommentId must be positive");
            }
            DailyProblemComment replyTarget = dailyProblemCommentRepository.findCommentById(replyCommentId)
                    .orElseThrow(() -> new BusinessException(404, "reply comment not found"));
            if (!safeDate.equals(replyTarget.dailyProblemDate()) || !safeProblemKey.equals(replyTarget.problemKey())) {
                throw new BusinessException(409, "reply comment belongs to another daily problem");
            }
            replyToUsername = userRepository.findById(replyTarget.userId())
                    .map(User::getUsername)
                    .orElseThrow(() -> new BusinessException(404, "reply comment author not found"));
        } else {
            validateRootCommentTarget(safeDate, safeProblemKey);
        }

        DailyProblemComment created = dailyProblemCommentRepository.insertComment(
                safeDate,
                safeProblemKey,
                user.getId(),
                replyCommentId,
                normalizedContent
        );

        return new DailyProblemCommentItem(
                created.id(),
                created.dailyProblemDate().toString(),
                created.problemKey(),
                created.content(),
                created.createdAt(),
                created.replyCommentId(),
                replyToUsername,
                new DailyProblemCommentAuthor(user.getId(), user.getUsername(), user.getAvatarUrl()),
                List.of()
        );
    }

    private DailyProblemCommentItem toCommentItem(DailyProblemCommentRepository.DailyProblemCommentRecord row,
                                                  String replyToUsername,
                                                  List<DailyProblemCommentItem> replies) {
        return new DailyProblemCommentItem(
                row.id(),
                row.dailyProblemDate().toString(),
                row.problemKey(),
                row.content(),
                row.createdAt(),
                row.replyCommentId(),
                replyToUsername,
                new DailyProblemCommentAuthor(row.userId(), row.authorUsername(), row.authorAvatarUrl()),
                replies
        );
    }

    private LocalDate requireDailyProblemDate(LocalDate dailyProblemDate) {
        if (dailyProblemDate == null) {
            throw new BusinessException(400, "dailyProblemDate is required");
        }
        return dailyProblemDate;
    }

    private String normalizeProblemKey(String problemKey) {
        if (problemKey == null || problemKey.isBlank()) {
            throw new BusinessException(400, "problemKey is required");
        }
        return problemKey.trim();
    }

    private String normalizeContent(String content) {
        if (content == null) {
            throw new BusinessException(400, "content must not be blank");
        }
        String normalized = content.trim();
        if (normalized.isEmpty()) {
            throw new BusinessException(400, "content must not be blank");
        }
        if (normalized.length() > 1000) {
            throw new BusinessException(400, "content length must be <= 1000");
        }
        return normalized;
    }

    private void validateRootCommentTarget(LocalDate dailyProblemDate, String problemKey) {
        if (dailyProblemCommentRepository.existsCommentsByDailyProblem(dailyProblemDate, problemKey)) {
            return;
        }
        if (dailyProblemRepository.existsDailyProblemInstance(dailyProblemDate, problemKey)) {
            return;
        }
        throw new BusinessException(404, "daily problem instance not found");
    }

    private Long findRootCommentId(Long commentId,
                                   Map<Long, DailyProblemCommentRepository.DailyProblemCommentRecord> commentById) {
        DailyProblemCommentRepository.DailyProblemCommentRecord current = commentById.get(commentId);
        Set<Long> visited = new HashSet<>();
        while (current != null && current.replyCommentId() != null && visited.add(current.id())) {
            DailyProblemCommentRepository.DailyProblemCommentRecord parent = commentById.get(current.replyCommentId());
            if (parent == null) {
                return current.id();
            }
            current = parent;
        }
        return current == null ? commentId : current.id();
    }

    private String resolveReplyToUsername(Long replyCommentId,
                                          Map<Long, DailyProblemCommentRepository.DailyProblemCommentRecord> commentById) {
        if (replyCommentId == null) {
            return null;
        }
        DailyProblemCommentRepository.DailyProblemCommentRecord parent = commentById.get(replyCommentId);
        return parent == null ? null : parent.authorUsername();
    }
}
