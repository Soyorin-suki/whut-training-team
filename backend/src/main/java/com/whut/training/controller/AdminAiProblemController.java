package com.whut.training.controller;

import com.whut.training.common.ApiResponse;
import com.whut.training.context.UserContext;
import com.whut.training.domain.dto.ai.AiProblemDtos;
import com.whut.training.service.AdminAiProblemService;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/ai-problems")
public class AdminAiProblemController {

    private final AdminAiProblemService adminAiProblemService;

    public AdminAiProblemController(AdminAiProblemService adminAiProblemService) {
        this.adminAiProblemService = adminAiProblemService;
    }

    @PostMapping("/sessions")
    public ApiResponse<AiProblemDtos.SessionDetailResponse> createSession(
            @Valid @RequestBody AiProblemDtos.SessionCreateRequest request
    ) {
        return ApiResponse.ok(adminAiProblemService.createSession(UserContext.getCurrentUser(), request));
    }

    @GetMapping("/sessions")
    public ApiResponse<AiProblemDtos.SessionPageResponse> listSessions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String page,
            @RequestParam(required = false) String pageSize
    ) {
        return ApiResponse.ok(adminAiProblemService.listSessions(UserContext.getCurrentUser(), keyword, status, page, pageSize));
    }

    @GetMapping("/sessions/{sessionId}")
    public ApiResponse<AiProblemDtos.SessionDetailResponse> getSessionDetail(@PathVariable Long sessionId) {
        return ApiResponse.ok(adminAiProblemService.getSessionDetail(UserContext.getCurrentUser(), sessionId));
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public ApiResponse<AiProblemDtos.SessionDetailResponse> appendMessage(
            @PathVariable Long sessionId,
            @Valid @RequestBody AiProblemDtos.MessageCreateRequest request
    ) {
        return ApiResponse.ok(adminAiProblemService.appendMessage(UserContext.getCurrentUser(), sessionId, request));
    }

    @PostMapping("/drafts/{draftId}/versions/{versionNo}/activate")
    public ApiResponse<AiProblemDtos.SessionDetailResponse> activateVersion(
            @PathVariable Long draftId,
            @PathVariable Integer versionNo
    ) {
        return ApiResponse.ok(adminAiProblemService.activateVersion(UserContext.getCurrentUser(), draftId, versionNo));
    }

    @PatchMapping("/drafts/{draftId}")
    public ApiResponse<AiProblemDtos.SessionDetailResponse> patchDraft(
            @PathVariable Long draftId,
            @RequestBody AiProblemDtos.DraftPatchRequest request
    ) {
        return ApiResponse.ok(adminAiProblemService.patchDraft(UserContext.getCurrentUser(), draftId, request));
    }

    @PostMapping("/drafts/{draftId}/artifacts/regenerate")
    public ApiResponse<AiProblemDtos.ArtifactBundleResponse> regenerateArtifacts(@PathVariable Long draftId) {
        return ApiResponse.ok(adminAiProblemService.regenerateArtifacts(UserContext.getCurrentUser(), draftId));
    }

    @GetMapping("/drafts/{draftId}/artifacts")
    public ApiResponse<AiProblemDtos.ArtifactBundleResponse> getArtifacts(@PathVariable Long draftId) {
        return ApiResponse.ok(adminAiProblemService.getArtifacts(UserContext.getCurrentUser(), draftId));
    }

    @GetMapping("/drafts/{draftId}/artifacts/download")
    public ResponseEntity<ByteArrayResource> downloadArtifacts(@PathVariable Long draftId) {
        AdminAiProblemService.DownloadPayload payload = adminAiProblemService.getDraftZip(UserContext.getCurrentUser(), draftId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + payload.fileName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(payload.content().length)
                .body(new ByteArrayResource(payload.content()));
    }
}
