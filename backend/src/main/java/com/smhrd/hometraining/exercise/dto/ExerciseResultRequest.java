package com.smhrd.hometraining.exercise.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ExerciseResultRequest(
        @NotBlank String exerciseType,
        @Min(0) int reps,
        @Min(0) int accuracy,
        @Min(0) int score,
        @Min(0) int perfectCount,
        @Min(0) int greatCount,
        @Min(0) int goodCount,
        @Min(0) int missCount
) {}
