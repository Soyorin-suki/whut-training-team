package com.whut.training.controller;

import com.whut.training.common.ApiResponse;
import com.whut.training.context.UserContext;
import com.whut.training.domain.dto.ActiveTeamTrainingDashboard;
import com.whut.training.domain.dto.TrainingExportRequest;
import com.whut.training.domain.entity.User;
import com.whut.training.domain.enums.UserRole;
import com.whut.training.exception.BusinessException;
import com.whut.training.service.TrainingDashboardService;
import com.whut.training.service.TrainingExcelExportService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员现役队员训练看板接口。
 */
@RestController
@RequestMapping("/api/admin/training-dashboard")
public class AdminTrainingDashboardController {

    private final TrainingDashboardService trainingDashboardService;
    private final TrainingExcelExportService trainingExcelExportService;

    public AdminTrainingDashboardController(
            TrainingDashboardService trainingDashboardService,
            TrainingExcelExportService trainingExcelExportService
    ) {
        this.trainingDashboardService = trainingDashboardService;
        this.trainingExcelExportService = trainingExcelExportService;
    }

    @GetMapping
    public ApiResponse<ActiveTeamTrainingDashboard> getDashboard() {
        requireAdmin();
        return ApiResponse.ok(trainingDashboardService.getDashboard());
    }

    @PostMapping("/export")
    public ResponseEntity<byte[]> export(@RequestBody TrainingExportRequest request) {
        requireAdmin();
        TrainingExcelExportService.ExportResult result = trainingExcelExportService.export(request);
        String encodedFilename = URLEncoder.encode(result.filename(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .contentLength(result.content().length)
                .body(result.content());
    }

    private void requireAdmin() {
        User currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(401, "unauthorized");
        }
        if (currentUser.getRole() != UserRole.ADMIN && currentUser.getRole() != UserRole.SUPER_ADMIN) {
            throw new BusinessException(403, "admin role required");
        }
    }
}
