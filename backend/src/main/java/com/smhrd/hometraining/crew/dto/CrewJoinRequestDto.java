package com.smhrd.hometraining.crew.dto;

import com.smhrd.hometraining.crew.entity.CrewJoinRequest;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CrewJoinRequestDto(
        Long id,
        Long requesterId,
        String requesterNickname,
        int requesterLevel,
        String message,
        LocalDateTime requestedAt
) {
    public static CrewJoinRequestDto from(CrewJoinRequest r) {
        return new CrewJoinRequestDto(r.getId(), r.getRequester().getId(), r.getRequester().getNickname(),
                r.getRequester().getLevel(), r.getMessage(), r.getRequestedAt());
    }

    public record Create(@Size(max = 200) String message) {}
}
