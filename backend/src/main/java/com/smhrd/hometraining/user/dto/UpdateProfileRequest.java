package com.smhrd.hometraining.user.dto;

public record UpdateProfileRequest(
        String nickname,
        String regionCity,
        String regionGu,
        String regionDong,
        String bio,
        Boolean profilePublic
) {}
