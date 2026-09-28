package com.smhrd.hometraining.crew.dto;

import com.smhrd.hometraining.crew.entity.CrewJoinRequest;

import java.time.LocalDateTime;

public record CrewJoinRequestDto(
        Long id,
        Long requesterId,
        String requesterNickname,
        int requesterLevel,
        LocalDateTime requestedAt
) {
    public static CrewJoinRequestDto from(CrewJoinRequest r) {
        return new CrewJoinRequestDto(r.getId(), r.getRequester().getId(), r.getRequester().getNickname(),
                r.getRequester().getLevel(), r.getRequestedAt());
    }
}
