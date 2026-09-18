package com.smhrd.hometraining.crew;

import com.smhrd.hometraining.common.exception.BusinessException;
import com.smhrd.hometraining.crew.dto.*;
import com.smhrd.hometraining.crew.entity.*;
import com.smhrd.hometraining.crew.repository.*;
import com.smhrd.hometraining.exercise.repository.ExerciseRecordRepository;
import com.smhrd.hometraining.user.UserService;
import com.smhrd.hometraining.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CrewService {

    private final CrewRepository crewRepository;
    private final CrewMemberRepository crewMemberRepository;
    private final CrewJoinRequestRepository crewJoinRequestRepository;
    private final CrewNoticeRepository crewNoticeRepository;
    private final CrewChatMessageRepository crewChatMessageRepository;
    private final ExerciseRecordRepository exerciseRecordRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;

    @Transactional(readOnly = true)
    public Optional<CrewResponse> getMyCrew(Long userId) {
        return crewMemberRepository.findByUserId(userId).map(cm -> toResponse(cm.getCrew()));
    }

    @Transactional(readOnly = true)
    public List<CrewSummaryResponse> browse(String regionCity, String regionGu, Long viewerUserId) {
        List<Crew> crews = (regionCity != null && regionGu != null)
                ? crewRepository.findByRegionCityAndRegionGuOrderByLevelDesc(regionCity, regionGu)
                : crewRepository.findAll();
        // 비로그인 상태로 둘러보는 경우(게스트)에는 viewerUserId가 없으니 항상 "가입요청하기"로 보인다.
        Set<Long> requestedCrewIds = viewerUserId == null ? Set.of()
                : crewJoinRequestRepository.findByRequesterId(viewerUserId).stream()
                    .map(r -> r.getCrew().getId()).collect(Collectors.toSet());
        return crews.stream()
                .map(c -> CrewSummaryResponse.of(c, crewMemberRepository.countByCrewId(c.getId()),
                        requestedCrewIds.contains(c.getId())))
                .toList();
    }

    @Transactional
    public CrewResponse create(Long userId, CrewCreateRequest req) {
        User user = userService.getUserOrThrow(userId);
        if (crewMemberRepository.findByUserId(userId).isPresent()) {
            throw new BusinessException("이미 크루에 소속되어 있습니다.");
        }
        if (user.getPoints() < Crew.CREATE_COST) {
            throw new BusinessException("크루 생성에는 " + Crew.CREATE_COST + "P가 필요합니다.");
        }
        if (crewRepository.existsByName(req.name())) {
            throw new BusinessException("이미 사용 중인 크루 이름입니다.");
        }
        user.setPoints(user.getPoints() - Crew.CREATE_COST);

        Crew crew = crewRepository.save(Crew.create(req.name(), req.description(), req.concept(),
                user.getRegionCity(), user.getRegionGu(), user.getRegionDong()));
        crewMemberRepository.save(CrewMember.of(crew, user, CrewMember.Role.LEADER));
        return toResponse(crew);
    }

    @Transactional
    public void requestJoin(Long userId, Long crewId, String message) {
        if (crewMemberRepository.findByUserId(userId).isPresent()) {
            throw new BusinessException("이미 크루에 소속되어 있습니다.");
        }
        Crew crew = getCrewOrThrow(crewId);
        if (crewJoinRequestRepository.existsByCrewIdAndRequesterId(crewId, userId)) {
            throw new BusinessException("이미 가입 신청을 보냈습니다.");
        }
        User requester = userService.getUserOrThrow(userId);
        crewJoinRequestRepository.save(CrewJoinRequest.of(crew, requester, message));
    }

    @Transactional(readOnly = true)
    public List<CrewJoinRequestDto> listJoinRequests(Long leaderId) {
        CrewMember me = requireLeader(leaderId);
        return crewJoinRequestRepository.findByCrewIdOrderByRequestedAtAsc(me.getCrew().getId())
                .stream().map(CrewJoinRequestDto::from).toList();
    }

    @Transactional
    public void approveJoinRequest(Long leaderId, Long requestId) {
        CrewMember leader = requireLeader(leaderId);
        CrewJoinRequest request = crewJoinRequestRepository.findById(requestId)
                .filter(r -> r.getCrew().getId().equals(leader.getCrew().getId()))
                .orElseThrow(() -> new BusinessException("가입 신청을 찾을 수 없습니다."));
        if (crewMemberRepository.findByUserId(request.getRequester().getId()).isPresent()) {
            throw new BusinessException("이미 다른 크루에 소속된 사용자입니다.");
        }
        crewMemberRepository.save(CrewMember.of(leader.getCrew(), request.getRequester(), CrewMember.Role.MEMBER));
        crewJoinRequestRepository.delete(request);
        // kick과 같은 방식으로 브로드캐스트 — 이미 크루에 접속해 있던 다른 크루원들 화면을
        // 새로고침 없이 갱신한다.
        messagingTemplate.convertAndSend("/topic/crews/" + leader.getCrew().getId() + "/members",
                new CrewMemberEventDto("JOINED", leader.getCrew().getId(),
                        request.getRequester().getId(), request.getRequester().getNickname()));
        // 승인된 본인은 방금 전까지 크루 소속이 아니었어서 위 토픽을 구독하고 있지 않다 —
        // 로그인 아이디(STOMP Principal의 이름, StompAuthChannelInterceptor 참고)로 개인 큐에
        // 따로 알려줘서, 가입 승인을 기다리는 화면(홈크루 탭)에 떠 있기만 하면 새로고침 없이도
        // 바로 우리 크루 화면으로 넘어가게 한다.
        messagingTemplate.convertAndSendToUser(request.getRequester().getLoginId(), "/queue/crew-events",
                new CrewMemberEventDto("JOINED", leader.getCrew().getId(),
                        request.getRequester().getId(), request.getRequester().getNickname()));
    }

    @Transactional
    public CrewResponse updateCrew(Long leaderId, CrewUpdateRequest req) {
        CrewMember leader = requireLeader(leaderId);
        Crew crew = leader.getCrew();
        if (req.description() != null) crew.setDescription(req.description());
        if (req.concept() != null) crew.setConcept(req.concept());
        return toResponse(crew);
    }

    @Transactional
    public void rejectJoinRequest(Long leaderId, Long requestId) {
        CrewMember leader = requireLeader(leaderId);
        CrewJoinRequest request = crewJoinRequestRepository.findById(requestId)
                .filter(r -> r.getCrew().getId().equals(leader.getCrew().getId()))
                .orElseThrow(() -> new BusinessException("가입 신청을 찾을 수 없습니다."));
        crewJoinRequestRepository.delete(request);
    }

    @Transactional
    public void kickMember(Long leaderId, Long targetUserId) {
        CrewMember leader = requireLeader(leaderId);
        if (leader.getUser().getId().equals(targetUserId)) {
            throw new BusinessException("크루장은 자기 자신을 강퇴할 수 없습니다.");
        }
        Long crewId = leader.getCrew().getId();
        CrewMember target = crewMemberRepository.findByUserId(targetUserId)
                .filter(cm -> cm.getCrew().getId().equals(crewId))
                .orElseThrow(() -> new BusinessException("크루원을 찾을 수 없습니다."));
        String targetNickname = target.getUser().getNickname();
        crewMemberRepository.deleteByCrewIdAndUserId(crewId, targetUserId);
        // 강퇴당한 사람 화면(같은 크루 화면을 보고 있다면)과 다른 크루원들의 크루원 목록을
        // 새로고침 없이 실시간으로 갱신하기 위해 크루채팅과 같은 방식으로 브로드캐스트한다.
        messagingTemplate.convertAndSend("/topic/crews/" + crewId + "/members",
                new CrewMemberEventDto("KICKED", crewId, targetUserId, targetNickname));
    }

    @Transactional
    public void leave(Long userId) {
        CrewMember me = crewMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("소속된 크루가 없습니다."));
        long memberCount = crewMemberRepository.countByCrewId(me.getCrew().getId());
        if (me.getRole() == CrewMember.Role.LEADER && memberCount > 1) {
            throw new BusinessException("크루장은 다른 팀원이 있는 동안 탈퇴할 수 없습니다. 먼저 강퇴하거나 크루장을 위임해주세요.");
        }
        crewMemberRepository.delete(me);
        if (memberCount <= 1) {
            crewRepository.deleteById(me.getCrew().getId());
        }
    }

    @Transactional(readOnly = true)
    public List<CrewNoticeDto> listNotices(Long userId) {
        CrewMember me = requireMember(userId);
        return crewNoticeRepository.findByCrewIdOrderByCreatedAtDesc(me.getCrew().getId())
                .stream().map(CrewNoticeDto::from).toList();
    }

    @Transactional
    public CrewNoticeDto addNotice(Long userId, CrewNoticeDto.Create req) {
        CrewMember leader = requireLeader(userId);
        CrewNotice notice = crewNoticeRepository.save(
                CrewNotice.of(leader.getCrew(), leader.getUser(), req.title(), req.body()));
        return CrewNoticeDto.from(notice);
    }

    @Transactional(readOnly = true)
    public List<CrewChatMessageDto> recentChat(Long userId) {
        CrewMember me = requireMember(userId);
        List<CrewChatMessage> messages = new ArrayList<>(
                crewChatMessageRepository.findTop50ByCrewIdOrderBySentAtDesc(me.getCrew().getId()));
        Collections.reverse(messages);
        return messages.stream().map(CrewChatMessageDto::from).toList();
    }

    @Transactional
    public CrewChatMessageDto sendChat(Long userId, Long crewId, String text) {
        CrewMember me = crewMemberRepository.findByUserId(userId)
                .filter(cm -> cm.getCrew().getId().equals(crewId))
                .orElseThrow(() -> new BusinessException("해당 크루의 멤버가 아닙니다."));
        CrewChatMessage saved = crewChatMessageRepository.save(CrewChatMessage.of(me.getCrew(), me.getUser(), text));
        return CrewChatMessageDto.from(saved);
    }

    private CrewResponse toResponse(Crew crew) {
        List<CrewMemberResponse> members = crewMemberRepository.findByCrewIdOrderByRoleAscJoinedAtAsc(crew.getId())
                .stream().map(CrewMemberResponse::from).toList();
        List<Long> memberIds = members.stream().map(CrewMemberResponse::userId).toList();
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        long current = memberIds.isEmpty() ? 0 : exerciseRecordRepository.sumRepsByUserIdsAndExerciseTypeAndPeriod(
                memberIds, crew.getGroupMissionExercise(), startOfDay, startOfDay.plusDays(1));
        return CrewResponse.of(crew, (int) current, members);
    }

    private Crew getCrewOrThrow(Long crewId) {
        return crewRepository.findById(crewId).orElseThrow(() -> new BusinessException("크루를 찾을 수 없습니다."));
    }

    private CrewMember requireMember(Long userId) {
        return crewMemberRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("소속된 크루가 없습니다."));
    }

    private CrewMember requireLeader(Long userId) {
        CrewMember member = requireMember(userId);
        if (member.getRole() != CrewMember.Role.LEADER) {
            throw new BusinessException("크루장만 가능한 작업입니다.");
        }
        return member;
    }
}
