package com.whut.training.service.impl;

import com.whut.training.aspect.annotation.ServiceLog;
import com.whut.training.domain.dto.ProblemCommentAuthor;
import com.whut.training.domain.dto.ProblemCommentItem;
import com.whut.training.domain.dto.ProblemCommentRequest;
import com.whut.training.domain.entity.ProblemComment;
import com.whut.training.domain.entity.User;
import com.whut.training.exception.BusinessException;
import com.whut.training.repository.DailyProblemRepository;
import com.whut.training.repository.ProblemCommentRepository;
import com.whut.training.repository.UserRepository;
import com.whut.training.service.ProblemCommentService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@ServiceLog
public class ProblemCommentServiceImpl implements ProblemCommentService {

    private final ProblemCommentRepository problemCommentRepository;
    private final DailyProblemRepository dailyProblemRepository;
    private final UserRepository userRepository;

    public ProblemCommentServiceImpl(ProblemCommentRepository problemCommentRepository,
                                     DailyProblemRepository dailyProblemRepository,
                                     UserRepository userRepository) {
        this.problemCommentRepository = problemCommentRepository;
        this.dailyProblemRepository = dailyProblemRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<ProblemCommentItem> getComments(String problemKey) {
        String safeProblemKey = normalizeProblemKey(problemKey);
        validateProblemExists(safeProblemKey);
        List<ProblemCommentRepository.ProblemCommentRecord> rows =
                problemCommentRepository.findCommentsByProblemKey(safeProblemKey);
        if (rows.isEmpty()) {
            return List.of();
        }

        Map<Long, ProblemCommentRepository.ProblemCommentRecord> commentById = new HashMap<>();
        for (ProblemCommentRepository.ProblemCommentRecord row : rows) {
            commentById.put(row.id(), row);
        }

        List<ProblemCommentRepository.ProblemCommentRecord> rootComments = new ArrayList<>();
        Map<Long, List<ProblemCommentItem>> repliesByRootId = new HashMap<>();
        for (ProblemCommentRepository.ProblemCommentRecord row : rows) {
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

        Comparator<ProblemCommentRepository.ProblemCommentRecord> rootComparator = Comparator
                .comparing(ProblemCommentRepository.ProblemCommentRecord::createdAt)
                .thenComparing(ProblemCommentRepository.ProblemCommentRecord::id)
                .reversed();
        Comparator<ProblemCommentItem> replyComparator = Comparator
                .comparing(ProblemCommentItem::createdAt)
                .thenComparing(ProblemCommentItem::id);

        rootComments.sort(rootComparator);
        repliesByRootId.values().forEach(items -> items.sort(replyComparator));

        List<ProblemCommentItem> items = new ArrayList<>();
        for (ProblemCommentRepository.ProblemCommentRecord rootComment : rootComments) {
            items.add(toCommentItem(
                    rootComment,
                    null,
                    List.copyOf(repliesByRootId.getOrDefault(rootComment.id(), List.of()))
            ));
        }
        return List.copyOf(items);
    }

    @Override
    public ProblemCommentItem createComment(User user, ProblemCommentRequest request) {
        if (user == null || user.getId() == null) {
            throw new BusinessException(401, "unauthorized");
        }
        String safeProblemKey = normalizeProblemKey(request == null ? null : request.problemKey());
        String normalizedContent = normalizeContent(request == null ? null : request.content());
        validateProblemExists(safeProblemKey);
        Long replyCommentId = request == null ? null : request.replyCommentId();

        String replyToUsername = null;
        if (replyCommentId != null) {
            if (replyCommentId <= 0) {
                throw new BusinessException(400, "replyCommentId must be positive");
            }
            ProblemComment replyTarget = problemCommentRepository.findCommentById(replyCommentId)
                    .orElseThrow(() -> new BusinessException(404, "reply comment not found"));
            if (!safeProblemKey.equals(replyTarget.problemKey())) {
                throw new BusinessException(409, "reply comment belongs to another problem");
            }
            replyToUsername = userRepository.findById(replyTarget.userId())
                    .map(User::getUsername)
                    .orElseThrow(() -> new BusinessException(404, "reply comment author not found"));
        }

        ProblemComment created = problemCommentRepository.insertComment(
                safeProblemKey,
                user.getId(),
                replyCommentId,
                normalizedContent
        );

        return new ProblemCommentItem(
                created.id(),
                created.problemKey(),
                created.content(),
                created.createdAt(),
                created.replyCommentId(),
                replyToUsername,
                new ProblemCommentAuthor(user.getId(), user.getUsername(), user.getAvatarUrl()),
                List.of()
        );
    }

    private ProblemCommentItem toCommentItem(ProblemCommentRepository.ProblemCommentRecord row,
                                             String replyToUsername,
                                             List<ProblemCommentItem> replies) {
        return new ProblemCommentItem(
                row.id(),
                row.problemKey(),
                row.content(),
                row.createdAt(),
                row.replyCommentId(),
                replyToUsername,
                new ProblemCommentAuthor(row.userId(), row.authorUsername(), row.authorAvatarUrl()),
                replies
        );
    }

    private String normalizeProblemKey(String problemKey) {
        if (problemKey == null || problemKey.isBlank()) {
            throw new BusinessException(400, "problemKey is required");
        }
        return problemKey.trim();
    }

    private void validateProblemExists(String problemKey) {
        if (!dailyProblemRepository.existsProblemKey(problemKey)) {
            throw new BusinessException(404, "problem not found");
        }
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

    private Long findRootCommentId(Long commentId,
                                   Map<Long, ProblemCommentRepository.ProblemCommentRecord> commentById) {
        ProblemCommentRepository.ProblemCommentRecord current = commentById.get(commentId);
        Set<Long> visited = new HashSet<>();
        while (current != null && current.replyCommentId() != null && visited.add(current.id())) {
            ProblemCommentRepository.ProblemCommentRecord parent = commentById.get(current.replyCommentId());
            if (parent == null) {
                return current.id();
            }
            current = parent;
        }
        return current == null ? commentId : current.id();
    }

    private String resolveReplyToUsername(Long replyCommentId,
                                          Map<Long, ProblemCommentRepository.ProblemCommentRecord> commentById) {
        if (replyCommentId == null) {
            return null;
        }
        ProblemCommentRepository.ProblemCommentRecord parent = commentById.get(replyCommentId);
        return parent == null ? null : parent.authorUsername();
    }
}
