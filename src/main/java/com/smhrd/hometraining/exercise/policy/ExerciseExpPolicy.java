package com.smhrd.hometraining.exercise.policy;

import com.smhrd.hometraining.common.exception.BusinessException;

public final class ExerciseExpPolicy {

    private ExerciseExpPolicy() {
        // 정책 클래스이므로 객체 생성을 막습니다.
    }

    /**
     * 서버에서 계산한 운동 점수를 이용하여
     * 지급할 경험치를 반환합니다.
     */
    public static int calculateExp(int score) {

        validateScore(score);

        if (score == 1500) {
            return 500;
        }

        if (score >= 1350) {
            return 450;
        }

        if (score >= 1200) {
            return 400;
        }

        if (score >= 1050) {
            return 350;
        }

        if (score >= 900) {
            return 300;
        }

        if (score >= 750) {
            return 250;
        }

        if (score >= 500) {
            return 150;
        }

        if (score >= 250) {
            return 50;
        }

        return 0;
    }

    /**
     * 운동 점수의 허용 범위를 검사합니다.
     */
    private static void validateScore(int score) {

        if (score < 0) {
            throw new BusinessException(
                    "운동 점수는 0 이상이어야 합니다."
            );
        }

        if (score > ExerciseScorePolicy.MAX_SCORE) {
            throw new BusinessException(
                    "운동 점수는 1,500점을 초과할 수 없습니다."
            );
        }
    }
}