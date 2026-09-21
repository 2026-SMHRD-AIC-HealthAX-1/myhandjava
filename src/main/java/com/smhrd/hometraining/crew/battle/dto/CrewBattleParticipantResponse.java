package com.smhrd.hometraining.crew.battle.dto;

import com.smhrd.hometraining.exercise.entity.ExerciseRecord;

public record CrewBattleParticipantResponse(

        Long userId,
        String nickname,

        Long crewId,
        String crewName,

        /**
         * MISS를 제외한 개인 인정 운동 횟수입니다.
         */
        int personalCount,

        /**
         * PERFECT, GREAT, GOOD, MISS를 모두 포함한
         * 개인 전체 시도 횟수입니다.
         */
        int totalAttempts,

        /**
         * 참가자가 대전에서 획득한 개인 누적 점수입니다.
         */
        long personalScore,

        /**
         * 가장 최근에 받은 자세 판정입니다.
         */
        ExerciseRecord.Grade latestGrade,

        int perfectCount,
        int greatCount,
        int goodCount,
        int missCount

) {
}