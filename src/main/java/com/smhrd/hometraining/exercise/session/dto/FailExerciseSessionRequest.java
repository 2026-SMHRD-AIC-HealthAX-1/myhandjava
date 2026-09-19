package com.smhrd.hometraining.exercise.session.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FailExerciseSessionRequest(

        @NotBlank(
                message = "운동 시작 실패 사유는 필수입니다."
        )
        @Size(
                max = 200,
                message = "운동 시작 실패 사유는 200자 이하여야 합니다."
        )
        String reason

) {
}