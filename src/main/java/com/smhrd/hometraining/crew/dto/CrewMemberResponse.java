package com.smhrd.hometraining.crew.dto;

import com.smhrd.hometraining.crew.entity.CrewMember;

public record CrewMemberResponse(
        Long userId,
        String nickname,
        String role,
        int level,
        long points
) {
    public static CrewMemberResponse from(CrewMember m) {
        return new CrewMemberResponse(m.getUser().getId(), m.getUser().getNickname(), m.getRole().name(),
                m.getUser().getLevel(), m.getUser().getPoints());
    }
}
