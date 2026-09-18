package com.smhrd.hometraining.user.dto;

import jakarta.validation.constraints.NotBlank;

/** profileJson: 프론트에서 계산한 관절 좌표·체형 프로필을 그대로 문자열(JSON)로 전달받는다. */
public record CalibrationRequest(@NotBlank String profileJson) {}
