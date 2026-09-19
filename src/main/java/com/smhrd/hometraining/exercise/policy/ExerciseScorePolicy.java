package com.smhrd.hometraining.exercise.policy;

import com.smhrd.hometraining.common.exception.BusinessException;

public final class ExerciseScorePolicy {

    public static final int MAX_REPS = 15;

    public static final int PERFECT_SCORE = 100;
    public static final int GREAT_SCORE = 80;
    public static final int GOOD_SCORE = 50;
    public static final int MISS_SCORE = 0;

    public static final int MAX_SCORE =
            MAX_REPS * PERFECT_SCORE;

    private ExerciseScorePolicy() {
        // 정책 클래스이므로 객체 생성을 막습니다.
    }

    /**
     * 자세 판정별 횟수를 이용하여 운동 점수를 계산합니다.
     *
     * PERFECT: 100점
     * GREAT: 80점
     * GOOD: 50점
     * MISS: 0점
     */
    public static int calculateScore(
            int perfectCount,
            int greatCount,
            int goodCount,
            int missCount
    ) {

        validateCounts(
                perfectCount,
                greatCount,
                goodCount,
                missCount
        );

        return (perfectCount * PERFECT_SCORE)
                + (greatCount * GREAT_SCORE)
                + (goodCount * GOOD_SCORE)
                + (missCount * MISS_SCORE);
    }

    /**
     * 자세 판정 횟수의 합계를 반환합니다.
     */
    public static int calculateTotalReps(
            int perfectCount,
            int greatCount,
            int goodCount,
            int missCount
    ) {

        validateNonNegative(
                perfectCount,
                greatCount,
                goodCount,
                missCount
        );

        return perfectCount
                + greatCount
                + goodCount
                + missCount;
    }

    /**
     * 프론트에서 전달된 전체 운동 횟수와
     * 자세별 횟수의 합계가 같은지 검사합니다.
     */
    public static void validateRepsMatch(
            int reps,
            int perfectCount,
            int greatCount,
            int goodCount,
            int missCount
    ) {

        if (reps < 0) {
            throw new BusinessException(
                    "전체 운동 횟수는 0 이상이어야 합니다."
            );
        }

        int totalCount =
                calculateTotalReps(
                        perfectCount,
                        greatCount,
                        goodCount,
                        missCount
                );

        if (reps != totalCount) {
            throw new BusinessException(
                    "전체 운동 횟수와 자세 판정 횟수의 합계가 일치하지 않습니다."
            );
        }

        if (reps > MAX_REPS) {
            throw new BusinessException(
                    "한 세트의 운동 횟수는 15회를 초과할 수 없습니다."
            );
        }
    }

    /**
     * 자세 판정 횟수가 정상적인지 검사합니다.
     */
    private static void validateCounts(
            int perfectCount,
            int greatCount,
            int goodCount,
            int missCount
    ) {

        validateNonNegative(
                perfectCount,
                greatCount,
                goodCount,
                missCount
        );

        int totalCount =
                perfectCount
                        + greatCount
                        + goodCount
                        + missCount;

        if (totalCount > MAX_REPS) {
            throw new BusinessException(
                    "자세 판정 횟수의 합계는 15회를 초과할 수 없습니다."
            );
        }
    }

    /**
     * 음수 횟수가 전달되는 것을 막습니다.
     */
    private static void validateNonNegative(
            int perfectCount,
            int greatCount,
            int goodCount,
            int missCount
    ) {

        if (perfectCount < 0
                || greatCount < 0
                || goodCount < 0
                || missCount < 0) {

            throw new BusinessException(
                    "자세 판정 횟수는 0 이상이어야 합니다."
            );
        }
    }
}