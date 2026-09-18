package com.smhrd.hometraining.mission.entity;

import java.util.function.IntFunction;

/** script.js의 SQUAT_MISSION_TEMPLATES 를 그대로 옮긴 일간 스쿼트 미션 템플릿. */
public enum MissionMetric {
    REPS(15, 30, 2.5, t -> "스쿼트 " + t + "회 달성"),
    PERFECT(3, 8, 4.0, t -> "스쿼트 퍼펙트 " + t + "개 만들기"),
    SESSIONS(1, 2, 35.0, t -> "스쿼트 세트 " + t + "회 완료"),
    MISS_FREE_SESSION(1, 1, 45.0, t -> "MISS 0회 세트 " + t + "회 달성"),
    ACC_SESSION(1, 2, 38.0, t -> "정확도 90% 이상 세트 " + t + "회");

    public final int minTarget;
    public final int maxTarget;
    public final double rewardPerUnit;
    public final IntFunction<String> labelFn;

    MissionMetric(int minTarget, int maxTarget, double rewardPerUnit, IntFunction<String> labelFn) {
        this.minTarget = minTarget;
        this.maxTarget = maxTarget;
        this.rewardPerUnit = rewardPerUnit;
        this.labelFn = labelFn;
    }

    public int rollTarget(java.util.random.RandomGenerator random) {
        return minTarget + random.nextInt(maxTarget - minTarget + 1);
    }

    public int rewardFor(int target) {
        return (int) (Math.round(target * rewardPerUnit / 10.0) * 10);
    }

    public String label(int target) {
        return labelFn.apply(target);
    }
}
