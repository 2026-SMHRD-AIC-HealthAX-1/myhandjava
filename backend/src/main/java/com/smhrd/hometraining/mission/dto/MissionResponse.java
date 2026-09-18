package com.smhrd.hometraining.mission.dto;

import java.time.LocalDate;

import com.smhrd.hometraining.mission.entity.Mission;

public record MissionResponse(

        Long id,
        String metric,
        String exerciseType,
        String label,

        int target,
        int current,

        int reward,
        int rewardExp,

        boolean achieved,
        boolean claimed,

        LocalDate assignedDate

) {

    /**
     * 사용자에게 배정된 미션과 현재 진행도를
     * 프론트엔드 응답 형태로 변환합니다.
     */
    public static MissionResponse of(
            Mission mission,
            int current
    ) {

        int safeCurrent =
                Math.max(current, 0);

        int displayedCurrent =
                Math.min(
                        safeCurrent,
                        mission.getTarget()
                );

        boolean achieved =
                safeCurrent >= mission.getTarget();

        return new MissionResponse(
                mission.getId(),
                mission.getMetric().name(),
                mission.getExerciseType(),
                mission.getLabel(),

                mission.getTarget(),
                displayedCurrent,

                mission.getReward(),
                mission.getRewardExp(),

                achieved,
                mission.isClaimed(),

                mission.getAssignedDate()
        );
    }
}