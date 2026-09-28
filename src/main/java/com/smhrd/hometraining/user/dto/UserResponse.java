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

        int nicknameTickets,
        int rankChallengeTickets,
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

        // 등급은 저장된 값을 그대로 믿지 않고 항상 순위 도전 누적 점수로 다시 계산한다 —
        // 점수 구간(UserGrade.forRankedScore)이 바뀌어도 예전에 저장된 값 때문에 화면에
        // 옛날 등급이 남아있는 일이 없게 하기 위함.
        UserGrade grade = UserGrade.forRankedScore(
                user.hasJoinedRankedChallenge(),
                user.getRankedScoreTotal()
        );

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

                user.getNicknameTickets(),
                user.getRankChallengeTickets(),
                user.getSetsUsedToday(),

                user.getBio(),
                user.getRole().name(),
                user.isProfilePublic()
        );
    }
}