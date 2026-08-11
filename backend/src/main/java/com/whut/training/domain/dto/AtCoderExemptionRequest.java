package com.whut.training.domain.dto;

import jakarta.validation.constraints.Size;

public record AtCoderExemptionRequest(
        boolean exempted,
        @Size(max = 500) String reason
) {}
