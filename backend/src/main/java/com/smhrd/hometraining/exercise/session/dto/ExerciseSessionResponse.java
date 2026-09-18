package com.smhrd.hometraining.exercise.session.dto;

import java.time.LocalDateTime;

import com.smhrd.hometraining.exercise.session.entity.ExerciseSession;

public record ExerciseSessionResponse(

        String sessionId,
        String exerciseType,

        String status,
        String statusName,

        boolean usageCharged,
        boolean resultSaved,

        boolean rewardEligible,
        boolean ticketUsed,

        long maxDurationSeconds,

        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime endedAt,

        String failureReason

) {

    /**
     * ExerciseSession 엔티티를
     * 프론트에 반환할 응답 객체로 변환합니다.
     */
    public static ExerciseSessionResponse from(
            ExerciseSession session
    ) {

        return new ExerciseSessionResponse(
                session.getSessionId(),
                session.getExerciseType(),

                session.getStatus().name(),
                session.getStatus().getKoreanName(),

                session.isUsageCharged(),
                session.isResultSaved(),

                session.isRewardEligible(),
                session.isTicketUsed(),

                ExerciseSession.MAX_DURATION_SECONDS,

                session.getCreatedAt(),
                session.getStartedAt(),
                session.getEndedAt(),

                session.getFailureReason()
        );
    }
}