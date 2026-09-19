package com.smhrd.hometraining.exercise.session.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateExerciseSessionRequest(

        @NotBlank(
                message = "운동 종류는 필수입니다."
        )
        @Size(
                max = 20,
                message = "운동 종류는 20자 이하여야 합니다."
        )
        String exerciseType

) {
}