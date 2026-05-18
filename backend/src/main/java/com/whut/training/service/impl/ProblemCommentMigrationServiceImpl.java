package com.whut.training.service.impl;

import com.whut.training.aspect.annotation.ServiceLog;
import com.whut.training.domain.entity.ProblemComment;
import com.whut.training.repository.ProblemCommentRepository;
import com.whut.training.service.ProblemCommentMigrationService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@ServiceLog
public class ProblemCommentMigrationServiceImpl implements ProblemCommentMigrationService {

    private final ProblemCommentRepository problemCommentRepository;

    public ProblemCommentMigrationServiceImpl(ProblemCommentRepository problemCommentRepository) {
        this.problemCommentRepository = problemCommentRepository;
    }

    @Override
    public void migrateLegacyComments() {
        List<ProblemCommentRepository.LegacyDailyProblemCommentRecord> legacyRows =
                problemCommentRepository.findLegacyComments();
        if (legacyRows.isEmpty()) {
            return;
        }

        Map<Long, Long> legacyToNewId = new HashMap<>();
        for (ProblemComment migrated : problemCommentRepository.findMigratedLegacyComments()) {
            if (migrated.legacyCommentId() != null && migrated.id() != null) {
                legacyToNewId.put(migrated.legacyCommentId(), migrated.id());
            }
        }

        for (ProblemCommentRepository.LegacyDailyProblemCommentRecord legacyRow : legacyRows) {
            if (legacyToNewId.containsKey(legacyRow.id())) {
                continue;
            }

            ProblemComment created = problemCommentRepository.insertMigratedComment(
                    legacyRow.problemKey(),
                    legacyRow.userId(),
                    legacyRow.content(),
                    legacyRow.createdAt(),
                    legacyRow.id()
            );
            if (created.id() != null) {
                legacyToNewId.put(legacyRow.id(), created.id());
            }
        }

        for (ProblemCommentRepository.LegacyDailyProblemCommentRecord legacyRow : legacyRows) {
            if (legacyRow.replyCommentId() == null) {
                continue;
            }
            Long migratedId = legacyToNewId.get(legacyRow.id());
            Long migratedReplyId = legacyToNewId.get(legacyRow.replyCommentId());
            if (migratedId == null || migratedReplyId == null) {
                continue;
            }
            problemCommentRepository.updateReplyCommentId(migratedId, migratedReplyId);
        }
    }
}
