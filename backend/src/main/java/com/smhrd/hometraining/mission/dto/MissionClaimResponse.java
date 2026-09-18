package com.smhrd.hometraining.mission.dto;

public record MissionClaimResponse(

        Long missionId,
        long pointsAwarded,
        int expAwarded

) {

    public static MissionClaimResponse of(
            Long missionId,
            long pointsAwarded,
            int expAwarded
    ) {

        return new MissionClaimResponse(
                missionId,
                pointsAwarded,
                expAwarded
        );
    }
}