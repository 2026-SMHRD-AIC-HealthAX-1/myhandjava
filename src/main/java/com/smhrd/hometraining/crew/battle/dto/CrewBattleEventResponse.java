package com.smhrd.hometraining.crew.battle.dto;

import java.time.LocalDateTime;

import com.smhrd.hometraining.exercise.entity.ExerciseRecord;

public record CrewBattleEventResponse(

        Long battleId,
        Long userId,
        Long crewId,

        /**
         * 이번 운동의 자세 판정입니다.
         */
        ExerciseRecord.Grade grade,

        /**
         * 해당 참가자의 인정 운동 횟수입니다.
         */
        int personalCount,

        /**
         * 해당 참가자의 누적 점수입니다.
         */
        long personalScore,

        /**
         * 해당 크루 전체 참가자의 인정 운동 횟수입니다.
         */
        long crewReps,

        /**
         * 해당 크루 전체 참가자의 합산 점수입니다.
         */
        long crewScore,

        /**
         * 이번 판정이 인정 운동 횟수에 포함됐는지 나타냅니다.
         * MISS일 때는 false입니다.
         */
        boolean counted,

        LocalDateTime occurredAt

) {
}