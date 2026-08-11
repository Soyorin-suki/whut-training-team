package com.whut.training.domain.dto;

public record AtCoderBindingResponse(
        String handle,
        String verificationToken,
        long expiresAtSeconds,
        String profileUrl,
        String instruction
) {
}
