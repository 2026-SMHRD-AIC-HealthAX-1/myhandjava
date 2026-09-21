package com.smhrd.hometraining.crew;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smhrd.hometraining.common.exception.BusinessException;
import com.smhrd.hometraining.crew.dto.CrewBattlePartyInviteEventDto;
import com.smhrd.hometraining.crew.dto.CrewBattlePartyResponseEventDto;
import com.smhrd.hometraining.crew.dto.CrewChatMessageDto;
import com.smhrd.hometraining.crew.dto.CrewChatReportResponse;
import com.smhrd.hometraining.crew.dto.CrewCreateRequest;
import com.smhrd.hometraining.crew.dto.CrewJoinRequestDto;
import com.smhrd.hometraining.crew.dto.CrewMemberEventDto;
import com.smhrd.hometraining.crew.dto.CrewMemberResponse;
import com.smhrd.hometraining.crew.dto.CrewNoticeDto;
import com.smhrd.hometraining.crew.dto.CrewResponse;
import com.smhrd.hometraining.crew.dto.CrewSummaryResponse;
import com.smhrd.hometraining.crew.dto.CrewUpdateRequest;
import com.smhrd.hometraining.crew.entity.Crew;
import com.smhrd.hometraining.crew.entity.CrewChatMessage;
import com.smhrd.hometraining.crew.entity.CrewChatReport;
import com.smhrd.hometraining.crew.entity.CrewJoinRequest;
import com.smhrd.hometraining.crew.entity.CrewMember;
import com.smhrd.hometraining.crew.entity.CrewNotice;
import com.smhrd.hometraining.crew.battle.repository.CrewBattleParticipantRepository;
import com.smhrd.hometraining.crew.repository.CrewChatMessageRepository;
import com.smhrd.hometraining.crew.repository.CrewChatReportRepository;
import com.smhrd.hometraining.crew.repository.CrewExperienceHistoryRepository;
import com.smhrd.hometraining.crew.repository.CrewJoinRequestRepository;
import com.smhrd.hometraining.crew.repository.CrewMemberRepository;
import com.smhrd.hometraining.crew.repository.CrewNoticeRepository;
import com.smhrd.hometraining.crew.repository.CrewRepository;
import com.smhrd.hometraining.crew.repository.CrewWeeklyContributionRepository;
import com.smhrd.hometraining.crew.repository.CrewWeeklyMissionRepository;
import com.smhrd.hometraining.exercise.repository.ExerciseRecordRepository;
import com.smhrd.hometraining.user.UserResourceHistoryService;
import com.smhrd.hometraining.user.entity.User;
import com.smhrd.hometraining.user.entity.UserResourceHistory.Reason;
import com.smhrd.hometraining.user.entity.UserResourceHistory.ResourceType;
import com.smhrd.hometraining.user.repository.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CrewService {

    private final CrewRepository crewRepository;
    private final CrewMemberRepository crewMemberRepository;
    private final CrewJoinRequestRepository crewJoinRequestRepository;
    private final CrewNoticeRepository crewNoticeRepository;
    private final CrewChatMessageRepository crewChatMessageRepository;
    private final CrewChatReportRepository crewChatReportRepository;
    private final CrewExperienceHistoryRepository crewExperienceHistoryRepository;
    private final CrewWeeklyMissionRepository crewWeeklyMissionRepository;
    private final CrewWeeklyContributionRepository crewWeeklyContributionRepository;
    private final CrewBattleParticipantRepository crewBattleParticipantRepository;
    private final ExerciseRecordRepository exerciseRecordRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final UserResourceHistoryService resourceHistoryService;
    private final CrewBattleContributionService
            crewBattleContributionService;
    private final EntityManager entityManager;

    /**
     * 현재 사용자가 가입한 크루를 조회합니다.
     */
    @Transactional(readOnly = true)
    public Optional<CrewResponse> getMyCrew(
            Long userId
    ) {

        return crewMemberRepository
                .findByUserId(userId)
                .map(crewMember ->
                        toResponse(
                                crewMember.getCrew()
                        )
                );
    }

    /**
     * 지역별 또는 전체 크루 목록을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<CrewSummaryResponse> browse(
            String regionCity,
            String regionGu,
            Long viewerUserId
    ) {

        List<Crew> crews =
                regionCity != null
                        && regionGu != null

                        ? crewRepository
                                .findByRegionCityAndRegionGuOrderByLevelDesc(
                                        regionCity,
                                        regionGu
                                )

                        : crewRepository.findAll();

        /*
         * 비로그인 상태로 둘러보는 경우에는
         * 가입 신청 여부를 확인하지 않습니다.
         */
        Set<Long> requestedCrewIds =
                viewerUserId == null
                        ? Set.of()
                        : crewJoinRequestRepository
                                .findByRequesterId(
                                        viewerUserId
                                )
                                .stream()
                                .map(request ->
                                        request.getCrew()
                                                .getId()
                                )
                                .collect(
                                        Collectors.toSet()
                                );

        return crews.stream()
                .map(crew ->
                        CrewSummaryResponse.of(
                                crew,
                                crewMemberRepository
                                        .countByCrewId(
                                                crew.getId()
                                        ),
                                requestedCrewIds.contains(
                                        crew.getId()
                                )
                        )
                )
                .toList();
    }

    /**
     * 새로운 크루를 생성합니다.
     *
     * 크루 생성과 포인트 차감,
     * 생성자의 크루장 등록은 하나의
     * 트랜잭션에서 처리합니다.
     */
    @Transactional
    public CrewResponse create(
            Long userId,
            CrewCreateRequest request
    ) {

        User user =
                getUserOrThrow(
                        userId
                );

        /*
         * 같은 사용자의 크루 생성 요청이
         * 동시에 들어오더라도 포인트가 중복 차감되거나
         * 크루가 여러 개 생성되지 않도록 잠급니다.
         */
        entityManager.lock(
                user,
                LockModeType.PESSIMISTIC_WRITE
        );

        if (crewMemberRepository
                .findByUserId(userId)
                .isPresent()) {

            throw new BusinessException(
                    "이미 크루에 소속되어 있습니다."
            );
        }

        if (user.getPoints() < Crew.CREATE_COST) {
            throw new BusinessException(
                    "크루 생성에는 "
                            + Crew.CREATE_COST
                            + "P가 필요합니다."
            );
        }

        if (crewRepository.existsByName(
                request.name()
        )) {

            throw new BusinessException(
                    "이미 사용 중인 크루 이름입니다."
            );
        }

        /*
         * 크루 생성 비용을 차감합니다.
         *
         * 아래 크루 또는 크루장 저장 과정에서 오류가 발생하면
         * 트랜잭션이 롤백되어 포인트도 다시 복구됩니다.
         */
        long pointsBefore = user.getPoints();

        user.setPoints(
                pointsBefore - Crew.CREATE_COST
        );

        resourceHistoryService.record(
                user,
                ResourceType.POINT,
                -Crew.CREATE_COST,
                pointsBefore,
                user.getPoints(),
                Reason.CREW_CREATE,
                request.name()
        );

        Crew crew =
                crewRepository.save(
                        Crew.create(
                                request.name(),
                                request.description(),
                                Crew.joinConcepts(request.concepts()),
                                user.getRegionCity(),
                                user.getRegionGu(),
                                user.getRegionDong()
                        )
                );

        /*
         * 크루를 생성한 사용자를
         * 자동으로 크루장으로 등록합니다.
         */
        crewMemberRepository.save(
                CrewMember.of(
                        crew,
                        user,
                        CrewMember.Role.LEADER
                )
        );

        return toResponse(crew);
    }

    /**
     * 크루 가입을 신청합니다.
     */
    @Transactional
    public void requestJoin(
            Long userId,
            Long crewId,
            String message
    ) {

        if (crewMemberRepository
                .findByUserId(userId)
                .isPresent()) {

            throw new BusinessException(
                    "이미 크루에 소속되어 있습니다."
            );
        }

        Crew crew =
                getCrewOrThrow(crewId);
        
        /*
         * 가입 신청이 비활성화된 크루는
         * 검색과 상세조회는 가능하지만
         * 신규 가입 신청은 받을 수 없습니다.
         */
        if (!crew.isJoinEnabled()) {
            throw new BusinessException(
                    "현재 가입 신청을 받지 않는 크루입니다."
            );
        }
        
        long currentMembers =
                crewMemberRepository
                        .countByCrewId(crewId);

        /*
         * 이미 최대 인원에 도달한 크루에는
         * 새로운 가입 신청을 등록하지 않습니다.
         */
        if (currentMembers >= Crew.MAX_MEMBERS) {
            throw new BusinessException(
                    "크루 정원이 모두 찼습니다."
            );
        }

        if (crewJoinRequestRepository
                .existsByCrewIdAndRequesterId(
                        crewId,
                        userId
                )) {

            throw new BusinessException(
                    "이미 가입 신청을 보냈습니다."
            );
        }

        User requester =
                getUserOrThrow(
                        userId
                );

        /*
         * 자동가입승인이 켜진 크루는 승인 대기열에 쌓지 않고
         * 신청과 동시에 바로 크루원으로 등록한다.
         */
        if (crew.isAutoApprove()) {
            addMemberAndNotify(crew, requester);
            return;
        }

        crewJoinRequestRepository.save(
                CrewJoinRequest.of(
                        crew,
                        requester,
                        message
                )
        );
    }

    /**
     * 크루 가입 신청 목록을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<CrewJoinRequestDto> listJoinRequests(
            Long leaderId
    ) {

        CrewMember leader =
                requireLeader(leaderId);

        return crewJoinRequestRepository
                .findByCrewIdOrderByRequestedAtAsc(
                        leader.getCrew().getId()
                )
                .stream()
                .map(CrewJoinRequestDto::from)
                .toList();
    }

    /**
     * 크루 가입 신청을 승인합니다.
     */
    @Transactional
    public void approveJoinRequest(
            Long leaderId,
            Long requestId
    ) {

        CrewMember leader =
                requireLeader(leaderId);

        CrewJoinRequest request =
                crewJoinRequestRepository
                        .findById(requestId)
                        .filter(joinRequest ->
                                joinRequest.getCrew()
                                        .getId()
                                        .equals(
                                                leader.getCrew()
                                                        .getId()
                                        )
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        "가입 신청을 찾을 수 없습니다."
                                )
                        );

        if (crewMemberRepository
                .findByUserId(
                        request.getRequester().getId()
                )
                .isPresent()) {

            throw new BusinessException(
                    "이미 다른 크루에 소속된 사용자입니다."
            );
        }

        if (crewMemberRepository
                .countByCrewId(
                        leader.getCrew().getId()
                )
                >= Crew.MAX_MEMBERS) {

            throw new BusinessException(
                    "크루는 최대 "
                            + Crew.MAX_MEMBERS
                            + "명까지 가입할 수 있습니다."
            );
        }

        addMemberAndNotify(
                leader.getCrew(),
                request.getRequester()
        );

        crewJoinRequestRepository.delete(request);
    }

    /**
     * 크루원을 추가하고, 기존 크루원과 새로 들어온 사용자 모두에게
     * 가입 완료를 실시간으로 알립니다.
     *
     * 크루장 승인(approveJoinRequest)과 자동가입승인(requestJoin) 두 경로에서 공통으로 씁니다.
     */
    private void addMemberAndNotify(
            Crew crew,
            User newMember
    ) {

        crewMemberRepository.save(
                CrewMember.of(
                        crew,
                        newMember,
                        CrewMember.Role.MEMBER
                )
        );

        /*
         * 기존 크루원의 화면에 신규 가입 정보를 전송합니다.
         */
        messagingTemplate.convertAndSend(
                "/topic/crews/"
                        + crew.getId()
                        + "/members",

                new CrewMemberEventDto(
                        "JOINED",
                        crew.getId(),
                        newMember.getId(),
                        newMember.getNickname()
                )
        );

        /*
         * 가입한 사용자에게도 개인 알림을 전송합니다.
         */
        messagingTemplate.convertAndSendToUser(
                newMember.getLoginId(),
                "/queue/crew-events",

                new CrewMemberEventDto(
                        "JOINED",
                        crew.getId(),
                        newMember.getId(),
                        newMember.getNickname()
                )
        );
    }

    /**
     * 크루 가입 신청 자동승인 여부를 변경합니다.
     *
     * 크루장만 변경할 수 있습니다.
     */
    @Transactional
    public CrewResponse updateAutoApprove(
            Long leaderId,
            boolean autoApprove
    ) {

        CrewMember leader =
                requireLeader(leaderId);

        Crew crew =
                leader.getCrew();

        crew.setAutoApprove(autoApprove);

        return toResponse(crew);
    }
    
    /**
     * 크루 설명과 콘셉트를 수정합니다.
     */
    @Transactional
    public CrewResponse updateCrew(
            Long leaderId,
            CrewUpdateRequest request
    ) {

        CrewMember leader =
                requireLeader(leaderId);

        Crew crew =
                leader.getCrew();

        if (request.description() != null) {
            crew.setDescription(
                    request.description()
            );
        }

        if (request.concepts() != null) {
            crew.setConcept(
                    Crew.joinConcepts(request.concepts())
            );
        }

        return toResponse(crew);
    }

    /**
     * 크루 가입 신청을 거절합니다.
     */
    @Transactional
    public void rejectJoinRequest(
            Long leaderId,
            Long requestId
    ) {

        CrewMember leader =
                requireLeader(leaderId);
        
        Crew crew =
                leader.getCrew();

        /*
         * 같은 크루의 가입 승인 요청이 동시에 들어와도
         * 최대 인원 5명을 초과하지 않도록
         * 크루 DB 행을 잠급니다.
         */
        entityManager.lock(
                crew,
                LockModeType.PESSIMISTIC_WRITE
        );

        CrewJoinRequest request =
                crewJoinRequestRepository
                        .findById(requestId)
                        .filter(joinRequest ->
                                joinRequest.getCrew()
                                        .getId()
                                        .equals(
                                                leader.getCrew()
                                                        .getId()
                                        )
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        "가입 신청을 찾을 수 없습니다."
                                )
                        );

        crewJoinRequestRepository.delete(request);
    }
    
    /**
     * 현재 크루장이 다른 크루원에게
     * 크루장 권한을 양도합니다.
     *
     * 기존 크루장은 일반 크루원이 되고,
     * 대상 크루원은 새로운 크루장이 됩니다.
     */
    @Transactional
    public CrewResponse transferLeadership(
            Long currentLeaderId,
            Long targetUserId
    ) {

        CrewMember currentLeader =
                requireLeader(currentLeaderId);

        if (currentLeaderId.equals(targetUserId)) {
            throw new BusinessException(
                    "자기 자신에게 크루장 권한을 양도할 수 없습니다."
            );
        }

        Crew crew =
                currentLeader.getCrew();

        /*
         * 같은 크루에서 권한 양도 요청이 동시에 들어와도
         * 크루장이 여러 명 생기지 않도록 잠급니다.
         */
        entityManager.lock(
                crew,
                LockModeType.PESSIMISTIC_WRITE
        );

        CrewMember newLeader =
                crewMemberRepository
                        .findByUserId(targetUserId)
                        .filter(member ->
                                member.getCrew()
                                        .getId()
                                        .equals(crew.getId())
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        "크루장으로 지정할 크루원을 찾을 수 없습니다."
                                )
                        );

        if (newLeader.getRole()
                != CrewMember.Role.MEMBER) {

            throw new BusinessException(
                    "일반 크루원에게만 크루장 권한을 양도할 수 있습니다."
            );
        }

        /*
         * 두 역할 변경은 같은 트랜잭션에서 처리됩니다.
         *
         * 중간에 오류가 발생하면 두 변경 모두 롤백됩니다.
         */
        currentLeader.demoteToMember();
        newLeader.promoteToLeader();

        /*
         * 크루원 화면이 새로고침 없이 갱신될 수 있도록
         * 새로운 크루장 정보를 전송합니다.
         */
        messagingTemplate.convertAndSend(
                "/topic/crews/"
                        + crew.getId()
                        + "/members",

                new CrewMemberEventDto(
                        "LEADER_CHANGED",
                        crew.getId(),
                        newLeader.getUser().getId(),
                        newLeader.getUser().getNickname()
                )
        );

        return toResponse(crew);
    }

    /**
     * 크루원을 강퇴합니다.
     */
    @Transactional
    public void kickMember(
            Long leaderId,
            Long targetUserId
    ) {

        CrewMember leader =
                requireLeader(leaderId);

        if (leader.getUser()
                .getId()
                .equals(targetUserId)) {

            throw new BusinessException(
                    "크루장은 자기 자신을 강퇴할 수 없습니다."
            );
        }

        Long crewId =
                leader.getCrew().getId();

        CrewMember target =
                crewMemberRepository
                        .findByUserId(targetUserId)
                        .filter(member ->
                                member.getCrew()
                                        .getId()
                                        .equals(crewId)
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        "크루원을 찾을 수 없습니다."
                                )
                        );

        String targetNickname =
                target.getUser().getNickname();

        crewBattleContributionService
                .handleMemberDeparture(
                        crewId,
                        targetUserId
                );

        crewMemberRepository
                .deleteByCrewIdAndUserId(
                        crewId,
                        targetUserId
                );

        messagingTemplate.convertAndSend(
                "/topic/crews/"
                        + crewId
                        + "/members",

                new CrewMemberEventDto(
                        "KICKED",
                        crewId,
                        targetUserId,
                        targetNickname
                )
        );
    }

    /**
     * 현재 사용자가 크루에서 탈퇴합니다.
     *
     * 일반 크루원은 바로 탈퇴할 수 있습니다.
     *
     * 크루장은 다른 크루원이 있으면 탈퇴할 수 없으며,
     * 먼저 크루장 권한을 양도해야 합니다.
     *
     * 크루장 혼자 남은 상태에서 탈퇴하면
     * 크루가 자동으로 해체됩니다.
     */
    @Transactional
    public void leave(Long userId) {

        CrewMember member =
                crewMemberRepository
                        .findByUserId(userId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "소속된 크루가 없습니다."
                                )
                        );

        Crew crew =
                member.getCrew();

        /*
         * 탈퇴 처리 중 가입 승인이나 다른 탈퇴가
         * 동시에 실행되지 않도록 크루 행을 잠급니다.
         */
        entityManager.lock(
                crew,
                LockModeType.PESSIMISTIC_WRITE
        );

        long memberCount =
                crewMemberRepository
                        .countByCrewId(
                                crew.getId()
                        );

        /*
         * 크루장이고 다른 크루원이 있다면
         * 권한을 먼저 양도해야 합니다.
         */
        if (member.isLeader()
                && memberCount > 1) {

            throw new BusinessException(
                    "다른 크루원이 있는 크루장은 탈퇴할 수 없습니다. "
                            + "먼저 다른 크루원에게 크루장 권한을 양도해주세요."
            );
        }

        /*
         * 일반 크루원은 가입 관계만 삭제합니다.
         * 사용자 계정은 삭제하지 않습니다.
         */
        if (!member.isLeader()) {

            crewBattleContributionService
                    .handleMemberDeparture(
                            crew.getId(),
                            userId
                    );

            crewMemberRepository.delete(member);
            return;
        }

        /*
         * 크루장 혼자 남은 경우에는
         * 크루원 관계와 크루를 함께 삭제합니다.
         */
        crewBattleContributionService
                .deleteCrewContributions(
                        crew.getId()
                );
        deleteCrewChildData(crew.getId());

        crewMemberRepository.delete(member);
        crewRepository.delete(crew);
    }

    /**
     * 크루장이 크루를 직접 해체합니다.
     *
     * 다른 크루원이 없는 경우에만 가능합니다.
     */
    @Transactional
    public void disband(Long leaderId) {

        CrewMember leader =
                requireLeader(leaderId);

        Crew crew =
                leader.getCrew();

        entityManager.lock(
                crew,
                LockModeType.PESSIMISTIC_WRITE
        );

        long memberCount =
                crewMemberRepository
                        .countByCrewId(
                                crew.getId()
                        );

        if (memberCount > 1) {
            throw new BusinessException(
                    "다른 크루원이 있는 상태에서는 크루를 해체할 수 없습니다."
            );
        }

        crewBattleContributionService
                .deleteCrewContributions(
                        crew.getId()
                );
        deleteCrewChildData(crew.getId());

        crewMemberRepository.delete(leader);
        crewRepository.delete(crew);
    }

    /**
     * 크루를 실제로 삭제하기 전에, crews를 참조하는 다른 테이블의 행을 먼저 지웁니다.
     *
     * 이 정리 없이 바로 crewRepository.delete(crew)를 호출하면 crew_chat_messages 등의
     * 외래키 제약에 걸려 삭제가 실패합니다.
     */
    private void deleteCrewChildData(Long crewId) {
        crewChatMessageRepository.deleteByCrewId(crewId);
        crewJoinRequestRepository.deleteByCrewId(crewId);
        crewNoticeRepository.deleteByCrewId(crewId);
        crewExperienceHistoryRepository.deleteByCrewId(crewId);
        crewWeeklyMissionRepository.deleteByCrewId(crewId);
    }

    /**
     * 회원탈퇴 시 크루 관련 데이터를 정리합니다.
     *
     * 크루 소속 여부와 무관하게 이 사용자가 남긴 채팅·공지·가입신청·대전참가 기록을 먼저
     * 지우고, 크루에 소속돼 있다면: 일반 크루원이면 탈퇴 처리, 크루장인데 혼자면 크루 자체를
     * 해체, 크루장인데 다른 크루원이 있으면 가장 먼저 가입한 크루원에게 자동으로 리더십을
     * 넘긴 뒤 탈퇴시킵니다(leave()와 달리 본인에게 양도를 요구하지 않고 자동으로 처리합니다).
     */
    @Transactional
    public void cleanupAndLeaveForWithdrawal(Long userId) {

        crewChatMessageRepository.deleteBySenderId(userId);
        crewNoticeRepository.deleteByAuthorId(userId);
        crewJoinRequestRepository.deleteByRequesterId(userId);
        crewBattleParticipantRepository.deleteByUser_Id(userId);
        /*
         * 평소 "크루 탈퇴"는 CrewBattleContributionPolicy(PRESERVE)에 따라 기여도 기록을
         * 크루에 남겨두지만, 계정 자체가 삭제되는 회원 탈퇴에서는 crew_battle_contributions.
         * user_id FK가 걸려 있어 그 행을 남겨두면 아래에서 User를 지울 때 그대로 실패한다
         * (회원탈퇴가 안 되던 원인). 이 사용자가 남긴 기여도는 모든 크루에서 무조건 지운다.
         */
        crewBattleContributionService.deleteAllForUser(userId);
        crewWeeklyContributionRepository.deleteByUserId(userId);

        Optional<CrewMember> memberOpt =
                crewMemberRepository.findByUserId(userId);

        if (memberOpt.isEmpty()) {
            return;
        }

        CrewMember member = memberOpt.get();
        Crew crew = member.getCrew();

        entityManager.lock(
                crew,
                LockModeType.PESSIMISTIC_WRITE
        );

        if (!member.isLeader()) {
            crewBattleContributionService.handleMemberDeparture(crew.getId(), userId);
            crewMemberRepository.delete(member);
            return;
        }

        long memberCount = crewMemberRepository.countByCrewId(crew.getId());

        if (memberCount <= 1) {
            crewBattleContributionService.deleteCrewContributions(crew.getId());
            deleteCrewChildData(crew.getId());
            crewMemberRepository.delete(member);
            crewRepository.delete(crew);
            return;
        }

        CrewMember nextLeader =
                crewMemberRepository
                        .findByCrewIdOrderByRoleAscJoinedAtAsc(crew.getId())
                        .stream()
                        .filter(m -> !m.getUser().getId().equals(userId))
                        .findFirst()
                        .orElseThrow(() ->
                                new BusinessException(
                                        "크루장 권한을 넘길 크루원을 찾을 수 없습니다."
                                )
                        );

        member.demoteToMember();
        nextLeader.promoteToLeader();

        messagingTemplate.convertAndSend(
                "/topic/crews/" + crew.getId() + "/members",
                new CrewMemberEventDto(
                        "LEADER_CHANGED",
                        crew.getId(),
                        nextLeader.getUser().getId(),
                        nextLeader.getUser().getNickname()
                )
        );

        crewBattleContributionService.handleMemberDeparture(crew.getId(), userId);
        crewMemberRepository.delete(member);
    }

    /**
     * 크루 공지사항을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<CrewNoticeDto> listNotices(
            Long userId
    ) {

        CrewMember me =
                requireMember(userId);

        return crewNoticeRepository
                .findByCrewIdOrderByCreatedAtDesc(
                        me.getCrew().getId()
                )
                .stream()
                .map(CrewNoticeDto::from)
                .toList();
    }

    /**
     * 크루 공지사항을 등록합니다.
     */
    @Transactional
    public CrewNoticeDto addNotice(
            Long userId,
            CrewNoticeDto.Create request
    ) {

        CrewMember leader =
                requireLeader(userId);

        CrewNotice notice =
                crewNoticeRepository.save(
                        CrewNotice.of(
                                leader.getCrew(),
                                leader.getUser(),
                                request.title(),
                                request.body()
                        )
                );

        return CrewNoticeDto.from(notice);
    }

    /**
     * 최근 크루 채팅 50개를 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<CrewChatMessageDto> recentChat(
            Long userId
    ) {

        CrewMember me =
                requireMember(userId);

        List<CrewChatMessage> messages =
                new ArrayList<>(
                        crewChatMessageRepository
                                .findTop50ByCrewIdOrderBySentAtDesc(
                                        me.getCrew().getId()
                                )
                );

        Collections.reverse(messages);

        return messages.stream()
                .map(CrewChatMessageDto::from)
                .toList();
    }

    /**
     * 크루 채팅 메시지를 저장합니다.
     */
    @Transactional
    public CrewChatMessageDto sendChat(
            Long userId,
            Long crewId,
            String text
    ) {

        CrewMember me =
                crewMemberRepository
                        .findByUserId(userId)
                        .filter(member ->
                                member.getCrew()
                                        .getId()
                                        .equals(crewId)
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        "해당 크루의 멤버가 아닙니다."
                                )
                        );

        CrewChatMessage saved =
                crewChatMessageRepository.save(
                        CrewChatMessage.of(
                                me.getCrew(),
                                me.getUser(),
                                text
                        )
                );

        return CrewChatMessageDto.from(saved);
    }

    /**
     * 크루채팅 메시지를 신고합니다.
     *
     * 신고자가 해당 메시지가 속한 크루의 멤버인지 확인한 뒤 저장한다 — 다른 크루 메시지는
     * 애초에 화면에 보이지 않지만, 혹시 모를 조작된 요청을 막기 위한 서버측 방어.
     */
    @Transactional
    public void reportChatMessage(
            Long userId,
            Long messageId
    ) {

        CrewMember me =
                requireMember(userId);

        CrewChatMessage message =
                crewChatMessageRepository
                        .findById(messageId)
                        .filter(m ->
                                m.getCrew()
                                        .getId()
                                        .equals(me.getCrew().getId())
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        "신고할 메시지를 찾을 수 없습니다."
                                )
                        );

        if (message.getSender().getId().equals(userId)) {
            throw new BusinessException(
                    "본인 메시지는 신고할 수 없습니다."
            );
        }

        crewChatReportRepository.save(
                CrewChatReport.of(
                        me.getCrew(),
                        message,
                        me.getUser(),
                        message.getSender()
                )
        );
    }

    /**
     * 관리자 "크루채팅 신고 관리" 화면의 전체 신고 목록을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<CrewChatReportResponse> listChatReportsForAdmin() {

        return crewChatReportRepository
                .findAllByOrderByReportedAtDesc()
                .stream()
                .map(CrewChatReportResponse::from)
                .toList();
    }

    /**
     * 신고를 처리완료로 표시합니다.
     */
    @Transactional
    public void resolveChatReport(Long reportId) {

        CrewChatReport report =
                crewChatReportRepository
                        .findById(reportId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "신고 내역을 찾을 수 없습니다."
                                )
                        );

        report.resolve();
    }

    /**
     * 크루대전 파티 초대를 대상 크루원들에게 개인 알림(큐)으로 전송합니다.
     *
     * 초대장을 DB에 저장하지 않는 휘발성 알림입니다 — 실제 대전 매칭 전
     * 팀을 꾸리는 단계일 뿐이라 서버는 중개만 하고 상태는 각자 클라이언트가 들고 있습니다.
     */
    @Transactional(readOnly = true)
    public void sendBattlePartyInvite(
            Long inviterUserId,
            Long crewId,
            int battleSize,
            List<Long> inviteeUserIds
    ) {

        CrewMember inviter =
                crewMemberRepository
                        .findByUserId(inviterUserId)
                        .filter(member ->
                                member.getCrew()
                                        .getId()
                                        .equals(crewId)
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        "해당 크루의 멤버가 아닙니다."
                                )
                        );

        for (Long inviteeUserId : inviteeUserIds) {

            crewMemberRepository
                    .findByUserId(inviteeUserId)
                    .filter(member ->
                            member.getCrew()
                                    .getId()
                                    .equals(crewId)
                    )
                    .ifPresent(invitee ->
                            messagingTemplate.convertAndSendToUser(
                                    invitee.getUser().getLoginId(),
                                    "/queue/crew-events",
                                    new CrewBattlePartyInviteEventDto(
                                            "BATTLE_PARTY_INVITE",
                                            crewId,
                                            battleSize,
                                            inviterUserId,
                                            inviter.getUser().getNickname()
                                    )
                            )
                    );
        }
    }

    /**
     * 크루대전 파티 초대에 대한 수락/거절 응답을 초대한 사람에게 전달합니다.
     */
    @Transactional(readOnly = true)
    public void respondBattlePartyInvite(
            Long responderUserId,
            Long crewId,
            Long inviterUserId,
            boolean accepted
    ) {

        CrewMember responder =
                crewMemberRepository
                        .findByUserId(responderUserId)
                        .filter(member ->
                                member.getCrew()
                                        .getId()
                                        .equals(crewId)
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        "해당 크루의 멤버가 아닙니다."
                                )
                        );

        crewMemberRepository
                .findByUserId(inviterUserId)
                .filter(member ->
                        member.getCrew()
                                .getId()
                                .equals(crewId)
                )
                .ifPresent(inviter ->
                        messagingTemplate.convertAndSendToUser(
                                inviter.getUser().getLoginId(),
                                "/queue/crew-events",
                                new CrewBattlePartyResponseEventDto(
                                        "BATTLE_PARTY_RESPONSE",
                                        crewId,
                                        responderUserId,
                                        responder.getUser().getNickname(),
                                        accepted
                                )
                        )
                );
    }

    /**
     * 크루 상세 응답을 만듭니다.
     */
    private CrewResponse toResponse(Crew crew) {

        List<CrewMemberResponse> members =
                crewMemberRepository
                        .findByCrewIdOrderByRoleAscJoinedAtAsc(
                                crew.getId()
                        )
                        .stream()
                        .map(CrewMemberResponse::from)
                        .toList();

        List<Long> memberIds =
                members.stream()
                        .map(CrewMemberResponse::userId)
                        .toList();

        LocalDateTime startOfDay =
                LocalDate.now().atStartOfDay();

        long current =
                memberIds.isEmpty()
                        ? 0
                        : exerciseRecordRepository
                                .sumRepsByUserIdsAndExerciseTypeAndPeriod(
                                        memberIds,
                                        crew.getGroupMissionExercise(),
                                        startOfDay,
                                        startOfDay.plusDays(1)
                                );

        return CrewResponse.of(
                crew,
                (int) current,
                members
        );
    }

    /**
     * 크루를 조회합니다.
     */
    private Crew getCrewOrThrow(Long crewId) {

        return crewRepository
                .findById(crewId)
                .orElseThrow(() ->
                        new BusinessException(
                                "크루를 찾을 수 없습니다."
                        )
                );
    }

    /**
     * 현재 사용자가 크루원인지 확인합니다.
     */
    private CrewMember requireMember(Long userId) {

        return crewMemberRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new BusinessException(
                                "소속된 크루가 없습니다."
                        )
                );
    }

    /**
     * 현재 사용자가 크루장인지 확인합니다.
     */
    private CrewMember requireLeader(Long userId) {

        CrewMember member =
                requireMember(userId);

        if (member.getRole()
                != CrewMember.Role.LEADER) {

            throw new BusinessException(
                    "크루장만 가능한 작업입니다."
            );
        }

        return member;
    }

    private User getUserOrThrow(Long userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new BusinessException(
                                "사용자를 찾을 수 없습니다."
                        )
                );
    }
}
