package com.smhrd.hometraining.crew.battle;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smhrd.hometraining.common.exception.BusinessException;
import com.smhrd.hometraining.crew.CrewBattleContributionService;
import com.smhrd.hometraining.crew.CrewExperienceService;
import com.smhrd.hometraining.crew.battle.dto.CrewBattleDto;
import com.smhrd.hometraining.crew.battle.dto.CrewBattleEventResponse;
import com.smhrd.hometraining.crew.battle.dto.CrewBattleParticipantResponse;
import com.smhrd.hometraining.crew.battle.dto.CrewBattleRepRequest;
import com.smhrd.hometraining.crew.battle.entity.CrewBattle;
import com.smhrd.hometraining.crew.battle.entity.CrewBattleParticipant;
import com.smhrd.hometraining.crew.battle.repository.CrewBattleParticipantRepository;
import com.smhrd.hometraining.crew.battle.repository.CrewBattleRepository;
import com.smhrd.hometraining.crew.entity.Crew;
import com.smhrd.hometraining.crew.entity.CrewExperienceHistory.SourceType;
import com.smhrd.hometraining.crew.entity.CrewMember;
import com.smhrd.hometraining.crew.repository.CrewMemberRepository;
import com.smhrd.hometraining.exercise.entity.ExerciseRecord;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import com.smhrd.hometraining.crew.battle.dto.CrewBattleResultResponse;

@Service
@RequiredArgsConstructor
public class CrewBattleService {

    private static final int MIN_TEAM_SIZE = 2;
    private static final int MAX_TEAM_SIZE = 5;
    private static final int MATCHING_CANDIDATE_LIMIT = 20;

    private static final Set<CrewBattle.Status> OPEN_STATUSES =
            EnumSet.of(
                    CrewBattle.Status.REQUESTED,
                    CrewBattle.Status.WAITING,
                    CrewBattle.Status.MATCHED,
                    CrewBattle.Status.ACTIVE
            );

    private final CrewBattleRepository battleRepository;
    private final CrewMemberRepository crewMemberRepository;
    private final CrewBattleParticipantRepository participantRepository;
    private final CrewBattleContributionService contributionService;
    private final CrewExperienceService crewExperienceService;
    private final EntityManager entityManager;

    /**
     * 자동 매칭을 신청합니다.
     */
    @Transactional
    public CrewBattleDto.Response request(
            Long userId,
            CrewBattleDto.CreateRequest request
    ) {

        if (request == null) {
            throw new BusinessException(
                    "자동 매칭 신청 정보가 필요합니다."
            );
        }

        CrewMember requester =
                requireMember(userId);

        Crew requesterCrew =
                requester.getCrew();

        /*
         * 같은 크루에서 동시에 여러 매칭을 신청하지 못하도록
         * 신청 크루의 DB 행을 잠급니다.
         */
        entityManager.lock(
                requesterCrew,
                LockModeType.PESSIMISTIC_WRITE
        );

        int teamSize =
                request.teamSize();

        requireTeamSize(teamSize);

        String exerciseType =
                normalizeExerciseType(
                        request.exerciseType()
                );

        Set<Long> participantUserIds =
                validateParticipants(
                        requesterCrew,
                        request.participantUserIds(),
                        teamSize,
                        userId,
                        "신청 크루"
                );

        ensureNoOpenBattle(
                requesterCrew.getId()
        );

        Long previousOpponentCrewId =
                findLastOpponentCrewId(
                        requesterCrew.getId()
                );

        List<CrewBattle> candidates =
                battleRepository
                        .findWaitingCandidatesForUpdate(
                                requesterCrew.getId(),
                                teamSize,
                                exerciseType,
                                previousOpponentCrewId,
                                CrewBattle.Status.WAITING,
                                PageRequest.of(
                                        0,
                                        MATCHING_CANDIDATE_LIMIT
                                )
                        );

        for (CrewBattle candidate : candidates) {

            if (!isValidWaitingCandidate(
                    candidate,
                    requesterCrew.getId(),
                    teamSize
            )) {

                if (candidate.isWaiting()) {
                    candidate.cancelWaiting();
                }

                continue;
            }

            Long candidateCrewId =
                    candidate.getChallenger()
                            .getId();

            Long candidatePreviousOpponentCrewId =
                    findLastOpponentCrewId(
                            candidateCrewId
                    );

            /*
             * 양쪽 크루 모두 직전 상대와 즉시 재대전할 수 없습니다.
             */
            if (requesterCrew.getId().equals(
                    candidatePreviousOpponentCrewId
            )) {
                continue;
            }

            candidate.matchOpponent(
                    requesterCrew,
                    participantUserIds
            );

            saveBattleParticipants(
                    candidate,
                    candidate.getChallengerUserIds(),
                    participantUserIds
            );

            candidate.start();

            return toResponse(candidate);
        }

        CrewBattle waitingBattle =
                CrewBattle.waitForMatching(
                        requesterCrew,
                        userId,
                        exerciseType,
                        participantUserIds
                );

        CrewBattle savedBattle =
                battleRepository.save(
                        waitingBattle
                );

        return toResponse(savedBattle);
    }

    /**
     * 본인이 신청한 자동 매칭 대기를 취소합니다.
     */
    @Transactional
    public CrewBattleDto.Response cancelMatching(
            Long userId,
            Long battleId
    ) {

        CrewBattle battle =
                getBattleForUpdate(battleId);

        if (!battle.isWaiting()) {
            throw new BusinessException(
                    "매칭 대기 중인 대전만 취소할 수 있습니다."
            );
        }

        if (battle.getRequesterUserId() == null
                || !battle.getRequesterUserId().equals(
                userId
        )) {

            throw new BusinessException(
                    "자동 매칭을 신청한 사용자만 취소할 수 있습니다."
            );
        }

        battle.cancelWaiting();

        return toResponse(battle);
    }

    /**
     * 기존 상대 지정 대전 수락 기능입니다.
     *
     * 현재는 자동 매칭만 허용합니다.
     */
    @Deprecated
    @Transactional
    public CrewBattleDto.Response accept(
            Long userId,
            Long battleId,
            CrewBattleDto.AcceptRequest request
    ) {

        throw new BusinessException(
                "크루대전은 자동 매칭으로만 진행할 수 있습니다."
        );
    }

    /**
     * 대전에 선택된 참가자 목록을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<CrewBattleParticipantResponse> getParticipants(
            long userId,
            Long battleId
    ) {

        CrewBattle battle =
                getBattle(battleId);

        if (battle.isWaiting()) {

            if (!battle.getChallengerUserIds()
                    .contains(userId)) {

                throw new BusinessException(
                        "크루대전 참가자가 아닙니다."
                );
            }

            return List.of();
        }

        participantRepository
                .findByBattle_IdAndUser_Id(
                        battleId,
                        userId
                )
                .orElseThrow(() ->
                        new BusinessException(
                                "크루대전 참가자가 아닙니다."
                        )
                );

        return participantRepository
                .findByBattle_Id(battleId)
                .stream()
                .map(participant ->
                new CrewBattleParticipantResponse(
                        participant.getUser().getId(),
                        participant.getUser().getNickname(),
                        participant.getCrew().getId(),
                        participant.getCrew().getName(),

                        participant.getValidCount(),
                        participant.getTotalAttempts(),
                        participant.getTotalScore(),

                        participant.getLatestGrade(),
                        participant.getPerfectCount(),
                        participant.getGreatCount(),
                        participant.getGoodCount(),
                        participant.getMissCount()
                )
                )
                .toList();
    }

    /**
     * 대전 한 건을 조회합니다.
     *
     * 제한시간이 지났다면 최종 결과를 저장합니다.
     */
    @Transactional
    public CrewBattleDto.Response get(
            Long userId,
            Long battleId
    ) {

        CrewMember member =
                requireMember(userId);

        CrewBattle battle =
                getBattleForUpdate(battleId);

        requireParticipatingCrew(
                member.getCrew().getId(),
                battle
        );

        finishIfExpired(battle);

        return toResponse(battle);
    }

    /**
     * 종료된 크루대전의 상세 결과를 조회합니다.
     */
    @Transactional
    public CrewBattleResultResponse getResult(
            Long userId,
            Long battleId
    ) {

        CrewMember member =
                requireMember(userId);

        CrewBattle battle =
                getBattleForUpdate(battleId);

        /*
         * 대전에 참여했던 양쪽 크루의 현재 크루원만
         * 상세 결과를 조회할 수 있습니다.
         */
        requireParticipatingCrew(
                member.getCrew().getId(),
                battle
        );

        /*
         * 제한시간이 지났지만 아직 종료 처리가 안 된 경우
         * 조회 요청에서 최종 결과를 저장합니다.
         */
        finishIfExpired(battle);

        if (!battle.isFinished()
                || !battle.isResultRecorded()) {

            throw new BusinessException(
                    "아직 종료되지 않은 크루대전입니다."
            );
        }

        if (battle.getOpponent() == null) {
            throw new BusinessException(
                    "상대 크루 정보가 없습니다."
            );
        }

        List<CrewBattleParticipantResponse>
                challengerParticipants =
                participantRepository
                        .findByBattle_IdAndCrew_Id(
                                battle.getId(),
                                battle.getChallenger().getId()
                        )
                        .stream()
                        .map(this::toParticipantResponse)
                        .toList();

        List<CrewBattleParticipantResponse>
                opponentParticipants =
                participantRepository
                        .findByBattle_IdAndCrew_Id(
                                battle.getId(),
                                battle.getOpponent().getId()
                        )
                        .stream()
                        .map(this::toParticipantResponse)
                        .toList();

        CrewBattleResultResponse.TeamResult
                challengerResult =
                new CrewBattleResultResponse.TeamResult(
                        battle.getChallenger().getId(),
                        battle.getChallenger().getName(),

                        battle.getChallengerResult(),

                        battle.getChallengerTotalReps(),
                        battle.getChallengerTotalScore(),

                        battle.getChallengerRewardExp(),

                        challengerParticipants
                );

        CrewBattleResultResponse.TeamResult
                opponentResult =
                new CrewBattleResultResponse.TeamResult(
                        battle.getOpponent().getId(),
                        battle.getOpponent().getName(),

                        battle.getOpponentResult(),

                        battle.getOpponentTotalReps(),
                        battle.getOpponentTotalScore(),

                        battle.getOpponentRewardExp(),

                        opponentParticipants
                );

        return new CrewBattleResultResponse(
                battle.getId(),
                battle.getStatus(),

                battle.getExerciseType(),
                battle.getTeamSize(),
                battle.getDurationMinutes(),

                battle.getCreatedAt(),
                battle.getMatchedAt(),
                battle.getStartedAt(),
                battle.getEndsAt(),
                battle.getFinishedAt(),

                challengerResult,
                opponentResult,

                battle.getWinnerCrewId(),
                battle.isDrawResult(),
                battle.isResultRecorded(),
                battle.isRewardRecorded()
        );
    }
    
    /**
     * 현재 크루의 대전 내역을 조회합니다.
     */
    @Transactional
    public List<CrewBattleDto.Response> getMine(
            Long userId
    ) {

        CrewMember member =
                requireMember(userId);

        List<CrewBattle> battles =
                battleRepository.findMine(
                        member.getCrew().getId()
                );

        return battles.stream()
                .map(battle -> {

                    finishIfExpired(battle);

                    return toResponse(battle);
                })
                .toList();
    }

    /**
     * 운동 1회의 자세 판정 결과를 대전에 반영합니다.
     */
    @Transactional
    public CrewBattleEventResponse registerRep(
            long userId,
            CrewBattleRepRequest request
    ) {

        if (request == null
                || request.battleId() == null
                || request.grade() == null) {

            throw new BusinessException(
                    "운동 판정 정보가 올바르지 않습니다."
            );
        }

        CrewBattle battle =
                getBattleForUpdate(
                        request.battleId()
                );

        /*
         * 판정 등록 전에 제한시간을 확인합니다.
         * 시간이 끝났다면 결과를 먼저 저장하고 판정을 거부합니다.
         */
        finishIfExpired(battle);

        if (!battle.isActive()) {
            throw new BusinessException(
                    "현재 진행 중인 크루대전이 아닙니다."
            );
        }

        CrewBattleParticipant participant =
                participantRepository
                        .findForUpdate(
                                request.battleId(),
                                userId
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        "크루대전 참가자가 아닙니다."
                                )
                        );

        participant.registerRep(
                request.grade()
        );

        /*
         * 변경된 참가자 점수를 합산 쿼리에 반영합니다.
         */
        entityManager.flush();

        boolean counted =
                request.grade()
                        != ExerciseRecord.Grade.MISS;

        /*
         * 해당 크루 참가자들의 인정 운동 횟수를 합산합니다.
         */
        long crewReps =
                participantRepository.sumValidCount(
                        battle.getId(),
                        participant.getCrew().getId()
                );

        /*
         * 해당 크루 참가자들의 점수를 합산합니다.
         */
        long crewScore =
                participantRepository.sumTotalScore(
                        battle.getId(),
                        participant.getCrew().getId()
                );

        /*
         * 목표 점수(teamSize * TARGET_SCORE_PER_MEMBER)를 먼저 채운 크루가 있으면
         * 제한시간 2분을 다 기다리지 않고 즉시 종료합니다.
         */
        if (crewScore >= CrewBattle.targetScoreFor(battle.getTeamSize())) {
            finishBattle(battle);
        }

        return new CrewBattleEventResponse(
                battle.getId(),
                participant.getUser().getId(),
                participant.getCrew().getId(),
                request.grade(),

                participant.getValidCount(),
                participant.getTotalScore(),

                crewReps,
                crewScore,

                counted,
                LocalDateTime.now()
        );
    }

    /**
     * 매칭된 양쪽 크루의 참가자를 저장합니다.
     */
    private void saveBattleParticipants(
            CrewBattle battle,
            Set<Long> challengerUserIds,
            Set<Long> opponentUserIds
    ) {

        for (Long participantUserId : challengerUserIds) {

            CrewMember member =
                    requireMember(
                            participantUserId
                    );

            if (!member.getCrew().getId().equals(
                    battle.getChallenger().getId()
            )) {

                throw new BusinessException(
                        "신청 크루에 소속되지 않은 참가자가 포함되어 있습니다."
                );
            }

            CrewBattleParticipant participant =
                    CrewBattleParticipant.create(
                            battle,
                            member.getUser(),
                            battle.getChallenger()
                    );

            participantRepository.save(
                    participant
            );
        }

        for (Long participantUserId : opponentUserIds) {

            CrewMember member =
                    requireMember(
                            participantUserId
                    );

            if (battle.getOpponent() == null
                    || !member.getCrew().getId().equals(
                    battle.getOpponent().getId()
            )) {

                throw new BusinessException(
                        "상대 크루에 소속되지 않은 참가자가 포함되어 있습니다."
                );
            }

            CrewBattleParticipant participant =
                    CrewBattleParticipant.create(
                            battle,
                            member.getUser(),
                            battle.getOpponent()
                    );

            participantRepository.save(
                    participant
            );
        }

        /*
         * 대전 시작 전에 모든 참가자 정보를 DB에 반영합니다.
         */
        participantRepository.flush();
    }

    /**
     * 대기 중인 상대 후보의 참가자 정보가
     * 현재도 유효한지 확인합니다.
     */
    private boolean isValidWaitingCandidate(
            CrewBattle candidate,
            Long requesterCrewId,
            int teamSize
    ) {

        if (candidate == null
                || !candidate.isWaiting()) {

            return false;
        }

        Crew candidateCrew =
                candidate.getChallenger();

        if (candidateCrew == null
                || candidateCrew.getId()
                        .equals(requesterCrewId)) {

            return false;
        }

        Set<Long> selectedUserIds =
                candidate.getChallengerUserIds();

        if (selectedUserIds == null
                || selectedUserIds.size()
                        != teamSize) {

            return false;
        }

        Long candidateRequesterUserId =
                candidate.getRequesterUserId();

        if (candidateRequesterUserId == null
                || !selectedUserIds.contains(
                candidateRequesterUserId
        )) {

            return false;
        }

        Set<Long> currentMemberIds =
                memberIds(
                        candidateCrew.getId()
                );

        return currentMemberIds.containsAll(
                selectedUserIds
        );
    }

    /**
     * 요청한 참가자들이 모두 같은 크루에 속해 있고
     * 신청자가 포함됐는지 확인합니다.
     */
    private Set<Long> validateParticipants(
            Crew crew,
            Set<Long> requestedUserIds,
            int teamSize,
            Long requesterUserId,
            String label
    ) {

        requireTeamSize(teamSize);

        if (requestedUserIds == null
                || requestedUserIds.stream()
                        .anyMatch(userId -> userId == null)) {

            throw new BusinessException(
                    label
                            + " 참가자 정보가 올바르지 않습니다."
            );
        }

        Set<Long> selectedUserIds =
                Set.copyOf(
                        requestedUserIds
                );

        if (selectedUserIds.size()
                != teamSize) {

            throw new BusinessException(
                    label
                            + "는 신청자를 포함하여 정확히 "
                            + teamSize
                            + "명을 선택해야 합니다."
            );
        }

        if (!selectedUserIds.contains(
                requesterUserId
        )) {

            throw new BusinessException(
                    "자동 매칭 신청자는 참가자에 반드시 포함되어야 합니다."
            );
        }

        Set<Long> currentMemberIds =
                memberIds(
                        crew.getId()
                );

        if (!currentMemberIds.containsAll(
                selectedUserIds
        )) {

            throw new BusinessException(
                    label
                            + "에 소속되지 않은 참가자가 포함되어 있습니다."
            );
        }

        return selectedUserIds;
    }

    private Set<Long> memberIds(
            Long crewId
    ) {

        return crewMemberRepository
                .findByCrewIdOrderByRoleAscJoinedAtAsc(
                        crewId
                )
                .stream()
                .map(member ->
                        member.getUser().getId()
                )
                .collect(Collectors.toSet());
    }

    /**
     * 가장 최근에 종료된 대전의 상대 크루 ID를 반환합니다.
     */
    private Long findLastOpponentCrewId(
            Long crewId
    ) {

        List<CrewBattle> recentBattles =
                battleRepository
                        .findLatestFinishedBattles(
                                crewId,
                                CrewBattle.Status.FINISHED,
                                PageRequest.of(0, 1)
                        );

        if (recentBattles.isEmpty()) {
            return null;
        }

        CrewBattle latestBattle =
                recentBattles.get(0);

        if (latestBattle.getChallenger()
                .getId()
                .equals(crewId)) {

            return latestBattle.getOpponent() == null
                    ? null
                    : latestBattle.getOpponent().getId();
        }

        return latestBattle.getChallenger()
                .getId();
    }

    private String normalizeExerciseType(
            String exerciseType
    ) {

        if (exerciseType == null
                || exerciseType.isBlank()) {

            throw new BusinessException(
                    "운동 종류가 필요합니다."
            );
        }

        String normalized =
                exerciseType.trim();

        if (normalized.length() > 20) {
            throw new BusinessException(
                    "운동 종류는 20자 이하여야 합니다."
            );
        }

        return normalized;
    }

    private void requireTeamSize(
            int teamSize
    ) {

        if (teamSize < MIN_TEAM_SIZE
                || teamSize > MAX_TEAM_SIZE) {

            throw new BusinessException(
                    "크루대전 인원은 2명부터 5명까지 선택할 수 있습니다."
            );
        }
    }

    private void ensureNoOpenBattle(
            Long crewId
    ) {

        if (battleRepository.existsOpenBattle(
                crewId,
                OPEN_STATUSES
        )) {

            throw new BusinessException(
                    "이미 대기 중이거나 진행 중인 크루대전이 있습니다."
            );
        }
    }

    private void requireParticipatingCrew(
            Long crewId,
            CrewBattle battle
    ) {

        boolean challenger =
                battle.getChallenger() != null
                        && battle.getChallenger()
                                .getId()
                                .equals(crewId);

        boolean opponent =
                battle.getOpponent() != null
                        && battle.getOpponent()
                                .getId()
                                .equals(crewId);

        if (!challenger && !opponent) {
            throw new BusinessException(
                    "이 대전을 조회할 권한이 없습니다."
            );
        }
    }

    private CrewBattle getBattle(
            Long battleId
    ) {

        if (battleId == null) {
            throw new BusinessException(
                    "크루대전 ID가 필요합니다."
            );
        }

        return battleRepository
                .findById(battleId)
                .orElseThrow(() ->
                        new BusinessException(
                                "크루대전을 찾을 수 없습니다."
                        )
                );
    }

    private CrewBattle getBattleForUpdate(
            Long battleId
    ) {

        if (battleId == null) {
            throw new BusinessException(
                    "크루대전 ID가 필요합니다."
            );
        }

        return battleRepository
                .findByIdForUpdate(battleId)
                .orElseThrow(() ->
                        new BusinessException(
                                "크루대전을 찾을 수 없습니다."
                        )
                );
    }

    private CrewMember requireMember(
            Long userId
    ) {

        return crewMemberRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new BusinessException(
                                "소속된 크루가 없습니다."
                        )
                );
    }

    /**
     * 제한시간이 지난 대전의 최종 횟수와 점수를 저장하고
     * 양쪽 크루에 결과 경험치를 지급합니다.
     */
    private void finishIfExpired(
            CrewBattle battle
    ) {

        if (battle == null
                || !battle.isActive()
                || battle.getEndsAt() == null
                || LocalDateTime.now()
                        .isBefore(battle.getEndsAt())) {

            return;
        }

        finishBattle(battle);
    }

    /**
     * 참가자 기록을 합산하여 최종 대전 결과를 저장합니다.
     */
    private void finishBattle(
            CrewBattle battle
    ) {

        if (battle == null
                || !battle.isActive()
                || battle.isResultRecorded()
                || battle.getOpponent() == null) {

            return;
        }

        Long challengerCrewId =
                battle.getChallenger().getId();

        Long opponentCrewId =
                battle.getOpponent().getId();

        /*
         * 크루별 인정 운동 횟수를 합산합니다.
         */
        long challengerReps =
                participantRepository.sumValidCount(
                        battle.getId(),
                        challengerCrewId
                );

        long opponentReps =
                participantRepository.sumValidCount(
                        battle.getId(),
                        opponentCrewId
                );

        /*
         * 크루별 실제 점수를 별도로 합산합니다.
         */
        long challengerScore =
                participantRepository.sumTotalScore(
                        battle.getId(),
                        challengerCrewId
                );

        long opponentScore =
                participantRepository.sumTotalScore(
                        battle.getId(),
                        opponentCrewId
                );

        battle.finishWithResult(
                challengerReps,
                opponentReps,
                challengerScore,
                opponentScore
        );

        contributionService.recordBattleContributions(
                battle
        );

        grantBattleExperience(battle);
    }

    /**
     * 저장된 최종 점수를 기준으로 양쪽 크루에 경험치를 지급하고,
     * 실제 지급된 경험치를 대전 결과에도 저장합니다.
     */
    private void grantBattleExperience(
            CrewBattle battle
    ) {

        if (battle.getOpponent() == null
                || !battle.isResultRecorded()
                || battle.isRewardRecorded()) {

            return;
        }

        Long challengerCrewId =
                battle.getChallenger().getId();

        Long opponentCrewId =
                battle.getOpponent().getId();

        long challengerScore =
                battle.getChallengerTotalScore();

        long opponentScore =
                battle.getOpponentTotalScore();

        int challengerRewardExp;
        int opponentRewardExp;

        if (challengerScore > opponentScore) {

            challengerRewardExp =
                    crewExperienceService.grantBattleReward(
                            challengerCrewId,
                            battle.getId(),
                            SourceType.BATTLE_WIN
                    );

            opponentRewardExp =
                    crewExperienceService.grantBattleReward(
                            opponentCrewId,
                            battle.getId(),
                            SourceType.BATTLE_LOSS
                    );

        } else if (challengerScore < opponentScore) {

            challengerRewardExp =
                    crewExperienceService.grantBattleReward(
                            challengerCrewId,
                            battle.getId(),
                            SourceType.BATTLE_LOSS
                    );

            opponentRewardExp =
                    crewExperienceService.grantBattleReward(
                            opponentCrewId,
                            battle.getId(),
                            SourceType.BATTLE_WIN
                    );

        } else {

            challengerRewardExp =
                    crewExperienceService.grantBattleReward(
                            challengerCrewId,
                            battle.getId(),
                            SourceType.BATTLE_DRAW
                    );

            opponentRewardExp =
                    crewExperienceService.grantBattleReward(
                            opponentCrewId,
                            battle.getId(),
                            SourceType.BATTLE_DRAW
                    );
        }

        /*
         * 주간 최대 경험치 제한까지 적용된
         * 실제 지급 경험치를 대전 결과에 저장합니다.
         */
        battle.recordRewardExperience(
                challengerRewardExp,
                opponentRewardExp
        );
    }
    
    /**
     * 참가자 엔티티를 결과 상세 응답으로 변환합니다.
     */
    private CrewBattleParticipantResponse toParticipantResponse(
            CrewBattleParticipant participant
    ) {

        return new CrewBattleParticipantResponse(
                participant.getUser().getId(),
                participant.getUser().getNickname(),

                participant.getCrew().getId(),
                participant.getCrew().getName(),

                participant.getValidCount(),
                participant.getTotalAttempts(),
                participant.getTotalScore(),

                participant.getLatestGrade(),

                participant.getPerfectCount(),
                participant.getGreatCount(),
                participant.getGoodCount(),
                participant.getMissCount()
        );
    }

    /**
     * 크루대전 상태와 점수를 응답으로 변환합니다.
     */
    private CrewBattleDto.Response toResponse(
            CrewBattle battle
    ) {

        Long opponentCrewId = null;
        String opponentCrewName = null;

        if (battle.getOpponent() != null) {

            opponentCrewId =
                    battle.getOpponent().getId();

            opponentCrewName =
                    battle.getOpponent().getName();
        }

        long challengerReps = 0L;
        long opponentReps = 0L;
        long challengerScore = 0L;
        long opponentScore = 0L;

        if (battle.isFinished()
                && battle.isResultRecorded()) {

            /*
             * 종료된 대전은 DB에 확정 저장된 결과를 반환합니다.
             */
            challengerReps =
                    battle.getChallengerTotalReps();

            opponentReps =
                    battle.getOpponentTotalReps();

            challengerScore =
                    battle.getChallengerTotalScore();

            opponentScore =
                    battle.getOpponentTotalScore();

        } else if (battle.getId() != null
                && !battle.isWaiting()) {

            /*
             * 진행 중인 대전은 참가자 기록에서
             * 현재 횟수와 점수를 실시간으로 합산합니다.
             */
            challengerReps =
                    participantRepository.sumValidCount(
                            battle.getId(),
                            battle.getChallenger().getId()
                    );

            challengerScore =
                    participantRepository.sumTotalScore(
                            battle.getId(),
                            battle.getChallenger().getId()
                    );

            if (opponentCrewId != null) {

                opponentReps =
                        participantRepository.sumValidCount(
                                battle.getId(),
                                opponentCrewId
                        );

                opponentScore =
                        participantRepository.sumTotalScore(
                                battle.getId(),
                                opponentCrewId
                        );
            }
        }

        /*
         * 기존 FINISHED 데이터 중 결과 저장 필드가 없는 경우를 위한
         * 임시 호환 처리입니다.
         */
        Long winnerCrewId =
                battle.getWinnerCrewId();

        boolean drawResult =
                battle.isDrawResult();

        if (battle.isFinished()
                && !battle.isResultRecorded()
                && opponentCrewId != null) {

            if (challengerScore > opponentScore) {

                winnerCrewId =
                        battle.getChallenger().getId();

                drawResult =
                        false;

            } else if (opponentScore > challengerScore) {

                winnerCrewId =
                        opponentCrewId;

                drawResult =
                        false;

            } else {

                winnerCrewId =
                        null;

                drawResult =
                        true;
            }
        }

        return new CrewBattleDto.Response(
                battle.getId(),
                battle.getStatus(),
                battle.getTeamSize(),
                CrewBattle.targetScoreFor(battle.getTeamSize()),

                battle.getRequesterUserId(),

                battle.getChallenger().getId(),
                battle.getChallenger().getName(),
                challengerReps,
                challengerScore,

                opponentCrewId,
                opponentCrewName,
                opponentReps,
                opponentScore,

                battle.getExerciseType(),

                battle.getCreatedAt(),
                battle.getMatchedAt(),
                battle.getStartedAt(),
                battle.getEndsAt(),
                battle.getFinishedAt(),

                remainingSeconds(battle),
                winnerCrewId,
                drawResult,
                battle.isResultRecorded()
        );
    }

    private long remainingSeconds(
            CrewBattle battle
    ) {

        if (!battle.isActive()
                || battle.getEndsAt() == null) {

            return 0L;
        }

        return Math.max(
                0L,
                Duration.between(
                        LocalDateTime.now(),
                        battle.getEndsAt()
                ).toSeconds()
        );
    }
}
