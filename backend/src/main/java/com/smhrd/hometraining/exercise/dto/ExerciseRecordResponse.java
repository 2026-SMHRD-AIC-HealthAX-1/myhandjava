package com.smhrd.hometraining.exercise.dto;

import java.time.LocalDateTime;

import com.smhrd.hometraining.exercise.entity.ExerciseRecord;
import com.smhrd.hometraining.exercise.session.entity.ExerciseSession;

public record ExerciseRecordResponse(

        Long id,

        String sessionId,
        String exerciseType,

        int reps,
        int accuracy,
        int score,
        String grade,

        int perfectCount,
        int greatCount,
        int goodCount,
        int missCount,

        int expAwarded,
        int pointsAwarded,

        boolean rewardEligible,
        boolean ticketUsed,

        LocalDateTime recordedAt

) {

    /**
     * 운동 기록 엔티티를
     * 프론트에 반환할 응답 객체로 변환합니다.
     */
    public static ExerciseRecordResponse from(
            ExerciseRecord record
    ) {

        ExerciseSession session =
                record.getSession();

        /*
         * 기존 운동 기록은 session이 없을 수 있습니다.
         */
        String sessionId =
                session == null
                        ? null
                        : session.getSessionId();

        /*
         * 세션 기능 적용 전 기존 기록은
         * 일반 보상 운동으로 처리합니다.
         */
        boolean rewardEligible =
                session == null
                        || session.isRewardEligible();

        boolean ticketUsed =
                session != null
                        && session.isTicketUsed();

        return new ExerciseRecordResponse(
                record.getId(),

                sessionId,
                record.getExerciseType(),

                record.getReps(),
                record.getAccuracy(),
                record.getScore(),
                record.getGrade().name(),

                record.getPerfectCount(),
                record.getGreatCount(),
                record.getGoodCount(),
                record.getMissCount(),

                record.getExpAwarded(),
                record.getPointsAwarded(),

                rewardEligible,
                ticketUsed,

                record.getRecordedAt()
        );
    }
}