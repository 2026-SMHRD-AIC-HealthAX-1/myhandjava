package com.smhrd.hometraining.crew.dto;

import java.time.LocalDateTime;

import com.smhrd.hometraining.crew.entity.CrewBattleContribution;

public record CrewBattleContributionResponse(

        Long crewId,
        Long userId,
        String nickname,
        long totalScore,
        boolean currentMember,
        LocalDateTime updatedAt

) {

    public static CrewBattleContributionResponse from(
            CrewBattleContribution contribution,
            boolean currentMember
    ) {

        return new CrewBattleContributionResponse(
                contribution.getCrew().getId(),
                contribution.getUser().getId(),
                contribution.getUser().getNickname(),
                contribution.getTotalScore(),
                currentMember,
                contribution.getUpdatedAt()
        );
    }
}
