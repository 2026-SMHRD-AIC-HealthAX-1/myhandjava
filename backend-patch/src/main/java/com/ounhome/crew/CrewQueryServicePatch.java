package com.ounhome.crew;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CrewQueryServicePatch {
    private final CrewRepository crews;
    private final CrewMemberRepository members;
    private final CrewJoinRequestRepositoryPatch requests;
    private final ObjectMapper json;

    public CrewQueryServicePatch(CrewRepository crews, CrewMemberRepository members,
            CrewJoinRequestRepositoryPatch requests, ObjectMapper json) {
        this.crews = crews; this.members = members; this.requests = requests; this.json = json;
    }

    public CrewDtos.Detail detail(Long crewId) {
        Crew crew = crews.findById(crewId).orElseThrow();
        Map<Long, CrewDtos.Member> result = new LinkedHashMap<>();
        User leader = crew.getLeader();
        result.put(leader.getId(), member(leader, "LEADER"));
        members.findAllByCrewId(crewId).forEach(row ->
            result.put(row.getUser().getId(), member(row.getUser(), row.getRole().name())));
        return new CrewDtos.Detail(crew.getId(), crew.getName(), crew.getDescription(),
            concepts(crew.getConceptsJson(), crew.getConcept()), crew.isJoinEnabled(),
            new ArrayList<>(result.values()));
    }

    public List<CrewDtos.JoinRequest> pending(Long crewId) {
        return requests.findAllByCrewIdAndStatusOrderByRequestedAtDesc(
            crewId, CrewJoinRequestStatus.PENDING).stream().map(r -> new CrewDtos.JoinRequest(
                r.getId(), r.getRequester().getId(), r.getRequester().getNickname(),
                r.getRequester().getLevel(), r.getMessage(), r.getStatus(), r.getRequestedAt())).toList();
    }

    public List<String> concepts(String conceptsJson, String legacy) {
        try {
            List<String> values = conceptsJson == null ? List.of() : json.readValue(conceptsJson, new TypeReference<>() {});
            return values.stream().filter(v -> v != null && !v.isBlank()).distinct().limit(3).toList();
        } catch (Exception ignored) {
            return legacy == null || legacy.isBlank() ? List.of() : List.of(legacy);
        }
    }

    private CrewDtos.Member member(User user, String role) {
        return new CrewDtos.Member(user.getId(), user.getNickname(), role, user.getLevel(), user.getPoints());
    }
}
