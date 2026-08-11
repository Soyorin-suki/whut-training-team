package com.whut.training.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AtCoderTrackingSettingRequest(
        @NotNull @Min(0) @Max(8) Integer minimumAcCount,
        @NotNull @Min(1) @Max(168) Integer graceHours
) {}
