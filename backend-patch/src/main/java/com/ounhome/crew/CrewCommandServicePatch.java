package com.ounhome.crew;

import java.time.LocalDateTime;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrewCommandServicePatch {
    private final CrewRepository crews;
    private final UserRepository users;
    private final CrewJoinRequestRepositoryPatch requests;
    private final SimpMessagingTemplate messaging;
    private final ObjectMapper json;

    public CrewCommandServicePatch(CrewRepository crews, UserRepository users,
            CrewJoinRequestRepositoryPatch requests, SimpMessagingTemplate messaging, ObjectMapper json) {
        this.crews = crews; this.users = users; this.requests = requests; this.messaging = messaging; this.json = json;
    }

    @Transactional
    public CrewJoinRequest requestJoin(Long crewId, Long requesterId, String message) {
        Crew crew = crews.findById(crewId).orElseThrow();
        if (!crew.isJoinEnabled()) throw new IllegalStateException("가입이 비활성화된 크루입니다.");
        if (requests.existsByCrewIdAndRequesterIdAndStatus(crewId, requesterId, CrewJoinRequestStatus.PENDING))
            throw new IllegalStateException("이미 대기 중인 가입 신청이 있습니다.");
        CrewJoinRequest request = new CrewJoinRequest();
        request.setCrew(crew);
        request.setRequester(users.findById(requesterId).orElseThrow());
        request.setMessage(message == null ? "" : message);
        request.setStatus(CrewJoinRequestStatus.PENDING);
        request.setRequestedAt(LocalDateTime.now());
        CrewJoinRequest saved = requests.saveAndFlush(request);
        messaging.convertAndSend("/topic/crews/" + crewId + "/join-requests",
            Map.of("type", "CREATED", "requestId", saved.getId(), "status", "PENDING"));
        return saved;
    }

    @Transactional
    public Crew saveConcepts(Long crewId, java.util.List<String> requested) {
        Crew crew = crews.findById(crewId).orElseThrow();
        java.util.List<String> concepts = requested == null ? java.util.List.of() : requested.stream()
            .filter(v -> v != null && !v.isBlank()).map(String::trim).distinct().limit(3).toList();
        if (concepts.isEmpty()) throw new IllegalArgumentException("크루 컨셉을 1개 이상 선택하세요.");
        try {
            crew.setConceptsJson(json.writeValueAsString(concepts));
            crew.setConcept(concepts.get(0)); // 구버전 클라이언트 호환
        } catch (Exception e) {
            throw new IllegalStateException("크루 컨셉 저장에 실패했습니다.", e);
        }
        return crew;
    }
}
