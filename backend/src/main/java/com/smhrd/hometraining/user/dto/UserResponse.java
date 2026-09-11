package com.smhrd.hometraining.user.dto;

import com.smhrd.hometraining.user.entity.User;

public record UserResponse(
        Long id,
        String loginId,
        String nickname,
        String email,
        String gender,
        int avatarIndex,
        String regionCity,
        String regionGu,
        String regionDong,
        int level,
        int exp,
        int expToNextLevel,
        long points,
        int streak,
        boolean streakRewardClaimed,
        int retakeTickets,
        int nicknameTickets,
        int extraSets,
        int setsUsedToday,
        String bio,
        String role,
        boolean profilePublic
) {
    public static final int EXP_PER_LEVEL = 1000;

    public static UserResponse from(User u) {
        return new UserResponse(
                u.getId(), u.getLoginId(), u.getNickname(), u.getEmail(), u.getGender().name(),
                u.getAvatarIndex(), u.getRegionCity(), u.getRegionGu(), u.getRegionDong(),
                u.getLevel(), u.getExp(), EXP_PER_LEVEL - u.getExp(), u.getPoints(),
                u.getStreak(), u.isStreakRewardClaimed(), u.getRetakeTickets(), u.getNicknameTickets(),
                u.getExtraSets(), u.getSetsUsedToday(), u.getBio(), u.getRole().name(), u.isProfilePublic());
    }
}
