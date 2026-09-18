package com.smhrd.hometraining.crew.policy;

public final class CrewBattleContributionPolicy {

    public enum LeaveHandling {
        PRESERVE,
        RESET
    }

    /**
     * 크루 탈퇴 또는 강퇴 후 기존 크루대전 기여도 처리 정책입니다.
     */
    public static final LeaveHandling LEAVE_HANDLING =
            LeaveHandling.PRESERVE;

    private CrewBattleContributionPolicy() {
    }

    public static boolean shouldResetOnLeave() {
        return LEAVE_HANDLING == LeaveHandling.RESET;
    }
}
