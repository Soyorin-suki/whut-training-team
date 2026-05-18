package com.whut.training.service;

import com.whut.training.domain.dto.ai.AiProblemDtos;
import com.whut.training.domain.entity.User;

public interface AdminAiProblemService {

    AiProblemDtos.SessionDetailResponse createSession(User adminUser, AiProblemDtos.SessionCreateRequest request);

    AiProblemDtos.SessionPageResponse listSessions(User adminUser, String keyword, String status, String page, String pageSize);

    AiProblemDtos.SessionDetailResponse getSessionDetail(User adminUser, Long sessionId);

    AiProblemDtos.SessionDetailResponse appendMessage(User adminUser, Long sessionId, AiProblemDtos.MessageCreateRequest request);

    AiProblemDtos.SessionDetailResponse activateVersion(User adminUser, Long draftId, Integer versionNo);

    AiProblemDtos.SessionDetailResponse patchDraft(User adminUser, Long draftId, AiProblemDtos.DraftPatchRequest request);

    AiProblemDtos.ArtifactBundleResponse regenerateArtifacts(User adminUser, Long draftId);

    AiProblemDtos.ArtifactBundleResponse getArtifacts(User adminUser, Long draftId);

    DownloadPayload getDraftZip(User adminUser, Long draftId);

    record DownloadPayload(String fileName, byte[] content) {
    }
}
