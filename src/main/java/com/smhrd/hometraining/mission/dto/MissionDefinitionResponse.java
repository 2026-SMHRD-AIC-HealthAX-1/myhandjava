package com.smhrd.hometraining.mission.dto;

import java.time.LocalDateTime;

import com.smhrd.hometraining.mission.entity.MissionDefinition;
import com.smhrd.hometraining.mission.entity.MissionMetric;
import com.smhrd.hometraining.mission.entity.MissionScope;

public record MissionDefinitionResponse(

        Long id,
        MissionScope scope,
        MissionMetric metric,
        String exerciseType,
        int minTarget,
        int maxTarget,
        String label,
        int rewardPoints,
        int rewardExp,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {

    public static MissionDefinitionResponse from(
            MissionDefinition definition
    ) {

        return new MissionDefinitionResponse(
                definition.getId(),
                definition.getScope(),
                definition.getMetric(),
                definition.getExerciseType(),
                definition.getMinTarget(),
                definition.getMaxTarget(),
                definition.getLabel(),
                definition.getRewardPoints(),
                definition.getRewardExp(),
                definition.isActive(),
                definition.getCreatedAt(),
                definition.getUpdatedAt()
        );
    }
}
