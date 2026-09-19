package com.smhrd.hometraining.user.dto;

import com.smhrd.hometraining.user.entity.User;
import com.smhrd.hometraining.user.entity.UserGrade;
import com.smhrd.hometraining.user.policy.UserLevelPolicy;

public record UserResponse(

        Long id,
        String loginId,
        String nickname,
        String email,

        String gender,
        int avatarIndex,
        boolean calibrationCompleted,

        String grade,
        String gradeName,

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

    public static UserResponse from(
            User user,
            boolean calibrationCompleted
    ) {

        String gender =
                user.getGender() == User.Gender.FEMALE
                        ? "female"
                        : "male";

        // 기존 회원에게 등급이 없으면 아이언 등급으로 처리합니다.
        UserGrade grade =
                user.getGrade() == null
                        ? UserGrade.IRON
                        : user.getGrade();

        // 현재 레벨에서 필요한 전체 경험치를 가져옵니다.
        int requiredExp =
                UserLevelPolicy.getRequiredExp(
                        user.getLevel()
                );

        // 다음 레벨까지 남은 경험치를 계산합니다.
        int expToNextLevel =
                Math.max(
                        requiredExp - user.getExp(),
                        0
                );

        return new UserResponse(
                user.getId(),
                user.getLoginId(),
                user.getNickname(),
                user.getEmail(),

                gender,
                user.getAvatarIndex(),
                calibrationCompleted,

                grade.name(),
                grade.getKoreanName(),

                user.getRegionCity(),
                user.getRegionGu(),
                user.getRegionDong(),

                user.getLevel(),
                user.getExp(),
                expToNextLevel,
                user.getPoints(),

                user.getStreak(),
                user.isStreakRewardClaimed(),

                user.getRetakeTickets(),
                user.getNicknameTickets(),
                user.getExtraSets(),
                user.getSetsUsedToday(),

                user.getBio(),
                user.getRole().name(),
                user.isProfilePublic()
        );
    }
}