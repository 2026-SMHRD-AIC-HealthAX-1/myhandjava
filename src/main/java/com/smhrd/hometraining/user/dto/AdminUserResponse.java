package com.smhrd.hometraining.user.dto;

import java.time.LocalDateTime;

import com.smhrd.hometraining.user.entity.User;

/** 관리자 "전체 사용자 관리" 화면의 회원 한 명 요약 정보입니다. */
public record AdminUserResponse(
        Long id,
        String nickname,
        String email,
        LocalDateTime createdAt,
        long exerciseCount,
        String status
) {

    public static AdminUserResponse of(User user, long exerciseCount) {
        return new AdminUserResponse(
                user.getId(),
                user.getNickname(),
                user.getEmail(),
                user.getCreatedAt(),
                exerciseCount,
                user.getStatus().name()
        );
    }
}
