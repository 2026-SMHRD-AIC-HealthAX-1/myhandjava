package com.ounhome.crew;

import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/crews")
public class CrewControllerPatch {
    private final CrewQueryServicePatch query;
    private final CrewCommandServicePatch command;
    private final CurrentUserService currentUser;

    public CrewControllerPatch(CrewQueryServicePatch query, CrewCommandServicePatch command,
            CurrentUserService currentUser) {
        this.query = query; this.command = command; this.currentUser = currentUser;
    }

    @GetMapping("/me")
    public CrewDtos.Detail myCrew(Principal principal) {
        return query.detail(currentUser.require(principal).getCrewId());
    }

    @GetMapping("/me/join-requests")
    public List<CrewDtos.JoinRequest> pending(Principal principal) {
        return query.pending(currentUser.requireLeader(principal).getCrewId());
    }

    @PostMapping("/{crewId}/join-requests")
    public CrewDtos.JoinRequest request(@PathVariable Long crewId,
            @RequestBody CrewDtos.CreateJoinRequest body, Principal principal) {
        var saved = command.requestJoin(crewId, currentUser.require(principal).getId(), body.message());
        return new CrewDtos.JoinRequest(saved.getId(), saved.getRequester().getId(),
            saved.getRequester().getNickname(), saved.getRequester().getLevel(), saved.getMessage(),
            saved.getStatus(), saved.getRequestedAt());
    }
}
