package com.smhrd.hometraining.exercise.dto;

import com.smhrd.hometraining.exercise.entity.ExerciseRecord;

import java.time.LocalDateTime;

public record ExerciseRecordResponse(
        Long id,
        String exerciseType,
        int reps,
        int accuracy,
        int score,
        String grade,
        int perfectCount,
        int greatCount,
        int goodCount,
        int missCount,
        int pointsAwarded,
        LocalDateTime recordedAt
) {
    public static ExerciseRecordResponse from(ExerciseRecord r) {
        return new ExerciseRecordResponse(r.getId(), r.getExerciseType(), r.getReps(), r.getAccuracy(),
                r.getScore(), r.getGrade().name(), r.getPerfectCount(), r.getGreatCount(), r.getGoodCount(),
                r.getMissCount(), r.getPointsAwarded(), r.getRecordedAt());
    }
}
