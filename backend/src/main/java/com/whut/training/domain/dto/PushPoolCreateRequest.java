package com.whut.training.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PushPoolCreateRequest(
        @NotBlank(message = "title is required")
        @Size(max = 255, message = "title length must be <= 255")
        String title,
        @NotBlank(message = "link is required")
        @Size(max = 1000, message = "link length must be <= 1000")
        String link,
        @Size(max = 2000, message = "description length must be <= 2000")
        String description
) {
}
