package com.smhrd.hometraining.crew.dto;

import java.util.List;

import com.smhrd.hometraining.crew.entity.Crew;

public record CrewResponse(

        Long id,
        String name,
        String description,
        List<String> concepts,
        String region,

        int level,
        int exp,

        boolean joinEnabled,
        boolean autoApprove,
        int currentMembers,
        int maxMembers,

        String groupMissionExercise,
        int groupMissionTarget,
        int groupMissionCurrent,

        List<CrewMemberResponse> members

) {

    public static CrewResponse of(
            Crew crew,
            int groupMissionCurrent,
            List<CrewMemberResponse> members
    ) {

        List<CrewMemberResponse> safeMembers =
                members == null
                        ? List.of()
                        : members;

        return new CrewResponse(
                crew.getId(),
                crew.getName(),
                crew.getDescription(),
                crew.conceptList(),
                crew.regionLabel(),

                crew.getLevel(),
                crew.getExp(),

                crew.isJoinEnabled(),
                crew.isAutoApprove(),
                safeMembers.size(),
                Crew.MAX_MEMBERS,

                crew.getGroupMissionExercise(),
                crew.getGroupMissionTarget(),
                groupMissionCurrent,

                safeMembers
        );
    }
}