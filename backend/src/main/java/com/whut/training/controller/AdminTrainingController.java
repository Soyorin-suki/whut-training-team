package com.whut.training.controller;

import com.whut.training.common.ApiResponse;
import com.whut.training.context.UserContext;
import com.whut.training.domain.dto.AdminDailyRecordDetailResponse;
import com.whut.training.domain.dto.AdminDailyRecordPageResponse;
import com.whut.training.domain.dto.AdminTrainingOverviewResponse;
import com.whut.training.domain.dto.AdminUserTimelineResponse;
import com.whut.training.domain.dto.AdminUserTrainingPageResponse;
import com.whut.training.domain.entity.User;
import com.whut.training.exception.BusinessException;
import com.whut.training.service.AdminTrainingService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/training")
public class AdminTrainingController {

    private static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    private final AdminTrainingService adminTrainingService;

    public AdminTrainingController(AdminTrainingService adminTrainingService) {
        this.adminTrainingService = adminTrainingService;
    }

    @GetMapping("/overview")
    public ApiResponse<AdminTrainingOverviewResponse> getOverview() {
        return ApiResponse.ok(adminTrainingService.getOverview(UserContext.getCurrentUser()));
    }

    @GetMapping("/export")
    public ResponseEntity<?> exportTrainingData() {
        try {
            AdminTrainingService.ExportPayload payload = adminTrainingService.exportTrainingData(UserContext.getCurrentUser());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + payload.fileName() + "\"")
                    .contentType(XLSX_MEDIA_TYPE)
                    .contentLength(payload.content().length)
                    .body(new ByteArrayResource(payload.content()));
        } catch (BusinessException ex) {
            return ResponseEntity.status(ex.getCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.fail(ex.getCode(), ex.getMessage()));
        }
    }

    @GetMapping("/daily-records")
    public ApiResponse<AdminDailyRecordPageResponse> getDailyRecords(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String page,
            @RequestParam(required = false) String pageSize
    ) {
        return ApiResponse.ok(adminTrainingService.getDailyRecords(
                UserContext.getCurrentUser(),
                startDate,
                endDate,
                page,
                pageSize
        ));
    }

    @GetMapping("/daily-records/{date}")
    public ApiResponse<AdminDailyRecordDetailResponse> getDailyRecordDetail(@PathVariable String date) {
        return ApiResponse.ok(adminTrainingService.getDailyRecordDetail(UserContext.getCurrentUser(), date));
    }

    @GetMapping("/users")
    public ApiResponse<AdminUserTrainingPageResponse> getUserTrainingPage(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String page,
            @RequestParam(required = false) String pageSize
    ) {
        return ApiResponse.ok(adminTrainingService.getUserTrainingPage(
                UserContext.getCurrentUser(),
                keyword,
                page,
                pageSize
        ));
    }

    @GetMapping("/users/{userId}/timeline")
    public ApiResponse<AdminUserTimelineResponse> getUserTimeline(
            @PathVariable Long userId,
            @RequestParam(required = false) String limit
    ) {
        return ApiResponse.ok(adminTrainingService.getUserTimeline(UserContext.getCurrentUser(), userId, limit));
    }
}
