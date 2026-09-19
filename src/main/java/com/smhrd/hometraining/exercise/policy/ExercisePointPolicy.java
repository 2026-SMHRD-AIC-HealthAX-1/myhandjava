package com.smhrd.hometraining.exercise.policy;

import com.smhrd.hometraining.common.exception.BusinessException;

public final class ExercisePointPolicy {

    /*
     * 현재 임시 포인트 지급 비율입니다.
     *
     * 최종 포인트 정책이 결정되면
     * 이 값이나 calculatePoints 메서드만 수정하면 됩니다.
     */
    private static final float POINT_RATE = 0.4f;

    private ExercisePointPolicy() {
        // 정책 클래스이므로 객체 생성을 막습니다.
    }

    /**
     * 서버에서 계산한 운동 점수를 이용하여
     * 지급할 포인트를 계산합니다.
     */
    public static int calculatePoints(int score) {

        validateScore(score);

        return Math.round(
                score * POINT_RATE
        );
    }

    /**
     * 포인트 계산에 사용할 운동 점수의
     * 허용 범위를 검사합니다.
     */
    private static void validateScore(int score) {

        if (score < 0) {
            throw new BusinessException(
                    "포인트 계산 점수는 0 이상이어야 합니다."
            );
        }

        if (score > ExerciseScorePolicy.MAX_SCORE) {
            throw new BusinessException(
                    "포인트 계산 점수는 1,500점을 초과할 수 없습니다."
            );
        }
    }
}