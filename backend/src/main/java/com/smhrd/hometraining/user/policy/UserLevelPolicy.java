package com.smhrd.hometraining.user.policy;

public final class UserLevelPolicy {

    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 500;

    private static final int START_REQUIRED_EXP = 100;
    private static final int LEVEL_SECTION_SIZE = 10;
    private static final int EXP_INCREASE_PER_SECTION = 50;

    // 500레벨 완료에 필요한 최대 경험치
    private static final int MAX_REQUIRED_EXP = 2500;

    /*
     * 배열의 번호를 레벨로 사용합니다.
     *
     * 예:
     * REQUIRED_EXP_BY_LEVEL[1] = 100
     * REQUIRED_EXP_BY_LEVEL[10] = 100
     * REQUIRED_EXP_BY_LEVEL[11] = 150
     * REQUIRED_EXP_BY_LEVEL[500] = 2500
     */
    private static final int[] REQUIRED_EXP_BY_LEVEL =
            createRequiredExpTable();

    private UserLevelPolicy() {
        // 객체 생성을 막기 위한 private 생성자
    }

    /**
     * 현재 레벨에서 다음 레벨로 올라가기 위해 필요한 경험치를 반환합니다.
     */
    public static int getRequiredExp(int level) {
        validateLevel(level);
        return REQUIRED_EXP_BY_LEVEL[level];
    }

    /**
     * 사용자가 현재 최고 레벨인지 확인합니다.
     */
    public static boolean isMaxLevel(int level) {
        return level == MAX_LEVEL;
    }

    /**
     * 레벨별 필요 경험치 표를 생성합니다.
     */
    private static int[] createRequiredExpTable() {

        int[] requiredExpTable = new int[MAX_LEVEL + 1];

        for (int level = MIN_LEVEL; level <= MAX_LEVEL; level++) {

            int section = (level - 1) / LEVEL_SECTION_SIZE;

            int calculatedExp =
                    START_REQUIRED_EXP
                            + (section * EXP_INCREASE_PER_SECTION);

            // 필요 경험치는 최대 2,500을 넘지 않도록 처리
            requiredExpTable[level] =
                    Math.min(calculatedExp, MAX_REQUIRED_EXP);
        }

        return requiredExpTable;
    }

    /**
     * 사용할 수 없는 레벨이 전달되면 오류를 발생시킵니다.
     */
    private static void validateLevel(int level) {

        if (level < MIN_LEVEL || level > MAX_LEVEL) {
            throw new IllegalArgumentException(
                    "레벨은 "
                            + MIN_LEVEL
                            + "부터 "
                            + MAX_LEVEL
                            + "까지만 사용할 수 있습니다."
            );
        }
    }
}