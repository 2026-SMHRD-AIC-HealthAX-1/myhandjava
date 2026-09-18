package com.smhrd.hometraining.mission.dto;

import com.smhrd.hometraining.mission.entity.MissionMetric;
import com.smhrd.hometraining.mission.entity.MissionScope;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MissionDefinitionRequest(

        @NotNull(message = "Mission scope is required.")
        MissionScope scope,

        @NotNull(message = "미션 종류는 필수입니다.")
        MissionMetric metric,

        @NotBlank(message = "운동 종류는 필수입니다.")
        @Size(
                max = 30,
                message = "운동 종류는 30자 이하여야 합니다."
        )
        String exerciseType,

        @Min(
                value = 1,
                message = "미션 목표값은 1 이상이어야 합니다."
        )
        int minTarget,

        @Min(
                value = 1,
                message = "Maximum target must be at least 1."
        )
        int maxTarget,

        @Min(
                value = 0,
                message = "Reward points cannot be negative."
        )
        int rewardPoints,

        @Min(
                value = 0,
                message = "Reward experience cannot be negative."
        )
        int rewardExp,

        @Size(
                max = 100,
                message = "미션 이름은 100자 이하여야 합니다."
        )
        String label

) {
}
