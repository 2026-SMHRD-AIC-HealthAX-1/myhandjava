package com.smhrd.hometraining.mission.entity;

import java.util.function.IntFunction;
import java.util.random.RandomGenerator;

import com.smhrd.hometraining.common.exception.BusinessException;

/**
 * 개인 일일 스쿼트 미션 종류입니다.
 *
 * 각 미션 종류마다 배정할 수 있는
 * 최소 목표값과 최대 목표값을 관리합니다.
 */
public enum MissionMetric {

    REPS(
            15,
            30,
            2.5,
            target -> "스쿼트 " + target + "회 달성"
    ),

    PERFECT(
            3,
            8,
            4.0,
            target -> "스쿼트 퍼펙트 " + target + "개 만들기"
    ),

    SESSIONS(
            1,
            2,
            35.0,
            target -> "스쿼트 세트 " + target + "회 완료"
    ),

    MISS_FREE_SESSION(
            1,
            1,
            45.0,
            target -> "MISS 0회 세트 " + target + "회 달성"
    ),

    ACC_SESSION(
            1,
            2,
            38.0,
            target -> "정확도 90% 이상 세트 " + target + "회"
    );

    private final int minTarget;
    private final int maxTarget;
    private final double rewardPerUnit;
    private final IntFunction<String> labelFunction;

    MissionMetric(
            int minTarget,
            int maxTarget,
            double rewardPerUnit,
            IntFunction<String> labelFunction
    ) {

        this.minTarget = minTarget;
        this.maxTarget = maxTarget;
        this.rewardPerUnit = rewardPerUnit;
        this.labelFunction = labelFunction;
    }

    /**
     * 설정된 최소·최대 범위 안에서
     * 임의의 목표값을 생성합니다.
     */
    public int rollTarget(RandomGenerator random) {

        if (random == null) {
            throw new BusinessException(
                    "미션 목표값 생성기가 필요합니다."
            );
        }

        return minTarget
                + random.nextInt(
                        maxTarget - minTarget + 1
                );
    }

    /**
     * 목표값이 해당 미션의 허용 범위 안인지 확인합니다.
     */
    public boolean isValidTarget(int target) {

        return target >= minTarget
                && target <= maxTarget;
    }

    /**
     * 목표값이 범위를 벗어나면 요청을 거부합니다.
     */
    public void validateTarget(int target) {

        if (!isValidTarget(target)) {
            throw new BusinessException(
                    name()
                            + " 미션 목표는 "
                            + minTarget
                            + " 이상 "
                            + maxTarget
                            + " 이하여야 합니다."
            );
        }
    }

    /**
     * 기존 미션 보상 계산과의 호환성을 위해 유지합니다.
     *
     * 이후 개인 일일 미션 보상은
     * 포인트 50, 경험치 50으로 통일합니다.
     */
    public int rewardFor(int target) {

        validateTarget(target);

        return (int) (
                Math.round(
                        target * rewardPerUnit / 10.0
                ) * 10
        );
    }

    /**
     * 미션 화면에 표시할 문구를 만듭니다.
     */
    public String label(int target) {

        validateTarget(target);

        return labelFunction.apply(target);
    }

    public int getMinTarget() {
        return minTarget;
    }

    public int getMaxTarget() {
        return maxTarget;
    }
}