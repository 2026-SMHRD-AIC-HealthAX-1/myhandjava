package com.ounhome.crew;

import java.time.LocalDateTime;
import java.util.List;

public final class CrewDtos {
    private CrewDtos() {}

    public record Member(Long userId, String nickname, String role, int level, long points) {}
    public record Detail(Long id, String name, String description, List<String> concepts,
                         boolean joinEnabled, List<Member> members) {}
    public record JoinRequest(Long id, Long requesterId, String requesterNickname,
                              int requesterLevel, String message, CrewJoinRequestStatus status,
                              LocalDateTime requestedAt) {}
    public record CreateJoinRequest(String message, CrewJoinRequestStatus status) {}
    public record SaveCrew(String name, String description, List<String> concepts) {}
}
