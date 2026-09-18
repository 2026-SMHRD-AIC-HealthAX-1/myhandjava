package com.smhrd.hometraining.crew.dto;

import com.smhrd.hometraining.crew.entity.Crew;

import java.util.List;

public record CrewResponse(
        Long id,
        String name,
        String description,
        String concept,
        String region,
        int level,
        int exp,
        String groupMissionExercise,
        int groupMissionTarget,
        int groupMissionCurrent,
        List<CrewMemberResponse> members
) {
    public static CrewResponse of(Crew crew, int groupMissionCurrent, List<CrewMemberResponse> members) {
        return new CrewResponse(crew.getId(), crew.getName(), crew.getDescription(), crew.getConcept(),
                crew.regionLabel(), crew.getLevel(), crew.getExp(), crew.getGroupMissionExercise(),
                crew.getGroupMissionTarget(), groupMissionCurrent, members);
    }
}
