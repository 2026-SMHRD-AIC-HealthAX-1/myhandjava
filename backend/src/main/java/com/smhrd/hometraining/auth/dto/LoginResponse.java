package com.smhrd.hometraining.auth.dto;

public record LoginResponse(
        String accessToken,
        Long userId,
        String nickname
) {}
