package com.smhrd.hometraining.crew.policy;

public final class CrewExperiencePolicy {

    /**
     * 크루 레벨업에 필요한 경험치입니다.
     */
    public static final int EXP_PER_LEVEL = 2000;

    /**
     * 크루 주간 미션 완료 보상입니다.
     */
    public static final int WEEKLY_MISSION_REWARD_EXP = 500;

    /**
     * 크루대전 승리 보상입니다.
     */
    public static final int BATTLE_WIN_EXP = 100;

    /**
     * 크루대전 패배 보상입니다.
     */
    public static final int BATTLE_LOSS_EXP = 50;

    /**
     * 무승부 보상은 아직 확정되지 않아
     * 현재 0으로 설정합니다.
     */
    public static final int BATTLE_DRAW_EXP = 0;

    /**
     * 크루대전으로 한 주 동안 획득할 수 있는
     * 최대 경험치입니다.
     */
    public static final int WEEKLY_BATTLE_EXP_LIMIT = 500;

    private CrewExperiencePolicy() {
    }

    /**
     * 주간 크루대전 경험치 한도를 적용해
     * 실제 지급할 경험치를 계산합니다.
     */
    public static int calculateBattleExp(
            int requestedExp,
            int alreadyAwardedThisWeek
    ) {

        int safeRequestedExp =
                Math.max(requestedExp, 0);

        int safeAlreadyAwarded =
                Math.max(alreadyAwardedThisWeek, 0);

        int remainingWeeklyLimit =
                Math.max(
                        WEEKLY_BATTLE_EXP_LIMIT
                                - safeAlreadyAwarded,
                        0
                );

        return Math.min(
                safeRequestedExp,
                remainingWeeklyLimit
        );
    }
}