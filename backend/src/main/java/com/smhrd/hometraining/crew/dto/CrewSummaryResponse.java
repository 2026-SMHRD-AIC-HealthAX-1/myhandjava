package com.smhrd.hometraining.crew.dto;

import com.smhrd.hometraining.crew.entity.Crew;

/**
 * 크루 가입 화면에서 목록으로 보여줄 요약 정보입니다.
 */
public record CrewSummaryResponse(

        Long id,
        String name,
        String description,
        String concept,
        String region,

        int level,

        long memberCount,
        int maxMembers,

        boolean joinEnabled,
        boolean full,
        boolean alreadyRequested

) {

    public static CrewSummaryResponse of(
            Crew crew,
            long memberCount,
            boolean alreadyRequested
    ) {

        boolean full =
                memberCount >= Crew.MAX_MEMBERS;

        return new CrewSummaryResponse(
                crew.getId(),
                crew.getName(),
                crew.getDescription(),
                crew.getConcept(),
                crew.regionLabel(),

                crew.getLevel(),

                memberCount,
                Crew.MAX_MEMBERS,

                crew.isJoinEnabled(),
                full,
                alreadyRequested
        );
    }
}