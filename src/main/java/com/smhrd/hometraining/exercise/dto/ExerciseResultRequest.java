package com.smhrd.hometraining.exercise.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ExerciseResultRequest(

        /**
         * 운동 세션 생성 API에서 발급받은 UUID입니다.
         */
        @NotBlank(
                message = "운동 세션 ID는 필수입니다."
        )
        @Pattern(
                regexp = "^[0-9a-fA-F]{8}-"
                        + "[0-9a-fA-F]{4}-"
                        + "[0-9a-fA-F]{4}-"
                        + "[0-9a-fA-F]{4}-"
                        + "[0-9a-fA-F]{12}$",
                message = "운동 세션 ID 형식이 올바르지 않습니다."
        )
        String sessionId,

        @NotBlank(
                message = "중복 방지 키는 필수입니다."
        )
        @Pattern(
                regexp = "^[A-Za-z0-9_-]{8,64}$",
                message = "중복 방지 키는 영문, 숫자, _, - 조합의 8~64자여야 합니다."
        )
        String idempotencyKey,

        @NotBlank(
                message = "운동 종류는 필수입니다."
        )
        @Size(
                max = 20,
                message = "운동 종류는 20자 이하여야 합니다."
        )
        String exerciseType,

        @Min(
                value = 0,
                message = "운동 횟수는 0 이상이어야 합니다."
        )
        @Max(
                value = 15,
                message = "한 세트의 운동 횟수는 15회를 초과할 수 없습니다."
        )
        int reps,

        @Min(
                value = 0,
                message = "정확도는 0 이상이어야 합니다."
        )
        @Max(
                value = 100,
                message = "정확도는 100을 초과할 수 없습니다."
        )
        int accuracy,

        /*
         * 기존 프론트엔드와의 호환성을 위해 남겨둡니다.
         *
         * 서버에서는 이 점수를 최종 점수로 신뢰하지 않고
         * 자세 판정 횟수로 다시 계산합니다.
         */
        @Min(
                value = 0,
                message = "점수는 0 이상이어야 합니다."
        )
        @Max(
                value = 1500,
                message = "점수는 1,500점을 초과할 수 없습니다."
        )
        int score,

        @Min(
                value = 0,
                message = "PERFECT 횟수는 0 이상이어야 합니다."
        )
        @Max(
                value = 15,
                message = "PERFECT 횟수는 15회를 초과할 수 없습니다."
        )
        int perfectCount,

        @Min(
                value = 0,
                message = "GREAT 횟수는 0 이상이어야 합니다."
        )
        @Max(
                value = 15,
                message = "GREAT 횟수는 15회를 초과할 수 없습니다."
        )
        int greatCount,

        @Min(
                value = 0,
                message = "GOOD 횟수는 0 이상이어야 합니다."
        )
        @Max(
                value = 15,
                message = "GOOD 횟수는 15회를 초과할 수 없습니다."
        )
        int goodCount,

        @Min(
                value = 0,
                message = "MISS 횟수는 0 이상이어야 합니다."
        )
        @Max(
                value = 15,
                message = "MISS 횟수는 15회를 초과할 수 없습니다."
        )
        int missCount,

        /**
         * 이 세션의 보상 등급입니다.
         *
         * FREE: 정상 지급. REDUCED: 포인트 미지급, 경험치는 1/3만 지급
         * (하루 6번째 운동부터). RANKED: 포인트·경험치 모두 미지급(순위 도전 모드).
         */
        @NotNull(
                message = "세션 보상 등급은 필수입니다."
        )
        SessionType sessionType

) {

    public enum SessionType {
        FREE,
        REDUCED,
        RANKED
    }

    /**
     * 기존 테스트 코드의 컴파일 오류를 방지하기 위한
     * 임시 생성자입니다.
     *
     * 실제 API 요청에는 sessionId가 반드시 필요합니다.
     */
    public ExerciseResultRequest(
            String sessionId,
            String exerciseType,
            int reps,
            int accuracy,
            int score,
            int perfectCount,
            int greatCount,
            int goodCount,
            int missCount
    ) {

        this(
                sessionId,
                java.util.UUID.randomUUID().toString(),
                exerciseType,
                reps,
                accuracy,
                score,
                perfectCount,
                greatCount,
                goodCount,
                missCount,
                SessionType.FREE
        );
    }

    public ExerciseResultRequest(
            String exerciseType,
            int reps,
            int accuracy,
            int score,
            int perfectCount,
            int greatCount,
            int goodCount,
            int missCount
    ) {

        this(
                null,
                java.util.UUID.randomUUID().toString(),
                exerciseType,
                reps,
                accuracy,
                score,
                perfectCount,
                greatCount,
                goodCount,
                missCount,
                SessionType.FREE
        );
    }
}
