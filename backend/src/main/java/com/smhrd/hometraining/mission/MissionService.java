package com.smhrd.hometraining.mission;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smhrd.hometraining.common.exception.BusinessException;
import com.smhrd.hometraining.mission.dto.MissionClaimResponse;
import com.smhrd.hometraining.mission.dto.MissionResponse;
import com.smhrd.hometraining.mission.entity.Mission;
import com.smhrd.hometraining.mission.entity.MissionCounter;
import com.smhrd.hometraining.mission.entity.MissionDefinition;
import com.smhrd.hometraining.mission.entity.MissionScope;
import com.smhrd.hometraining.mission.repository.MissionCounterRepository;
import com.smhrd.hometraining.mission.repository.MissionDefinitionRepository;
import com.smhrd.hometraining.mission.repository.MissionRepository;
import com.smhrd.hometraining.user.UserService;
import com.smhrd.hometraining.user.entity.User;
import com.smhrd.hometraining.user.entity.UserResourceHistory.Reason;
import com.smhrd.hometraining.user.repository.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MissionService {

    private static final int DAILY_MISSION_COUNT = 3;

    private static final ZoneId KOREA_ZONE_ID =
            ZoneId.of("Asia/Seoul");

    private final SecureRandom random =
            new SecureRandom();

    private final MissionRepository missionRepository;
    private final MissionCounterRepository counterRepository;

    private final MissionDefinitionRepository
            missionDefinitionRepository;

    private final UserRepository userRepository;
    private final UserService userService;
    private final EntityManager entityManager;

    /**
     * 오늘 사용자에게 배정된 일일 미션을 조회합니다.
     *
     * 오늘 배정된 미션이 3개보다 적으면
     * 활성 관리자 미션 중 중복되지 않는 미션을
     * 무작위로 선택해서 총 3개를 배정합니다.
     */
    @Transactional
    public List<MissionResponse> getTodayMissions(
            Long userId
    ) {

        LocalDate today =
                LocalDate.now(KOREA_ZONE_ID);

        User user =
                userService.getUserOrThrow(userId);

        /*
         * 같은 사용자가 동시에 미션 조회 요청을 보내도
         * 일일 미션이 중복 생성되지 않도록 잠급니다.
         */
        entityManager.lock(
                user,
                LockModeType.PESSIMISTIC_WRITE
        );

        List<Mission> missions =
                new ArrayList<>(
                        missionRepository
                                .findByUserIdAndAssignedDateOrderByIdAsc(
                                        userId,
                                        today
                                )
                );
        if (missions.size() < DAILY_MISSION_COUNT) {
            assignMissingMissions(
                    user,
                    today,
                    missions
            );
        }

        return missions.stream()
                .map(mission ->
                        MissionResponse.of(
                                mission,
                                getOrCreateCounter(
                                        userId,
                                        today,
                                        mission.getExerciseType()
                                ).valueOf(
                                        mission.getMetric()
                                )
                        )
                )
                .toList();
    }

    /**
     * 완료된 일일 미션의 보상을 수령합니다.
     *
     * 포인트와 경험치를 함께 지급하고
     * 지급 결과를 반환합니다.
     */
    @Transactional
    public MissionClaimResponse claim(
            Long userId,
            Long missionId
    ) {

        Mission mission =
                missionRepository
                        .findByIdAndUserId(
                                missionId,
                                userId
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        "미션을 찾을 수 없습니다."
                                )
                        );

        LocalDate today =
                LocalDate.now(KOREA_ZONE_ID);

        if (!today.equals(mission.getAssignedDate())) {
            throw new BusinessException(
                    "오늘 배정된 미션만 보상을 받을 수 있습니다."
            );
        }

        if (mission.isClaimed()) {
            throw new BusinessException(
                    "이미 보상을 받은 미션입니다."
            );
        }

        MissionCounter counter =
                getOrCreateCounter(
                        userId,
                        mission.getAssignedDate(),
                        mission.getExerciseType()
                );

        int current =
                counter.valueOf(
                        mission.getMetric()
                );

        if (current < mission.getTarget()) {
            throw new BusinessException(
                    "아직 목표를 달성하지 못했습니다."
            );
        }

        User user =
                mission.getUser();

        /*
         * 서로 다른 미션 보상을 동시에 요청해도
         * 경험치와 포인트가 유실되지 않도록
         * 사용자 DB 행을 잠급니다.
         */
        entityManager.lock(
                user,
                LockModeType.PESSIMISTIC_WRITE
        );

        mission.markClaimed();

        int expAwarded =
                mission.getRewardExp();

        long pointsAwarded =
                mission.getReward();

        userService.grantRewards(
                user,
                expAwarded,
                pointsAwarded,
                Reason.MISSION_REWARD,
                mission.getId()
        );

        return MissionClaimResponse.of(
                mission.getId(),
                pointsAwarded,
                expAwarded
        );
    }

    /**
     * 정상적으로 완료된 스쿼트 운동 결과가 저장될 때
     * 오늘의 개인 미션 진행도를 갱신합니다.
     *
     * 무료 운동과 다시찍기 티켓 운동을
     * 모두 일일 미션 진행도에 반영합니다.
     */
    @Transactional
    public void recordSquatSession(
            Long userId,
            int validReps,
            int perfectCount,
            int missCount,
            int accuracy
    ) {

        recordExerciseSession(
                userId,
                "SQUAT",
                validReps,
                perfectCount,
                missCount,
                accuracy
        );
    }

    @Transactional
    public void recordExerciseSession(
            Long userId,
            String exerciseType,
            int validReps,
            int perfectCount,
            int missCount,
            int accuracy
    ) {

        LocalDate today =
                LocalDate.now(KOREA_ZONE_ID);

        User user =
                userService.getUserOrThrow(userId);

        /*
         * 동시에 여러 결과가 저장되더라도
         * 미션 진행도 증가값이 유실되지 않도록 잠급니다.
         */
        entityManager.lock(
                user,
                LockModeType.PESSIMISTIC_WRITE
        );

        MissionCounter counter =
                getOrCreateCounter(
                        userId,
                        today,
                        exerciseType
                );

        counter.setReps(
                counter.getReps()
                        + Math.max(validReps, 0)
        );

        counter.setPerfect(
                counter.getPerfect()
                        + Math.max(perfectCount, 0)
        );

        counter.setSessions(
                counter.getSessions() + 1
        );

        if (missCount == 0) {
            counter.setMissFreeSession(
                    counter.getMissFreeSession() + 1
            );
        }

        if (accuracy >= 90) {
            counter.setAccSession(
                    counter.getAccSession() + 1
            );
        }
    }

    /**
     * 부족한 오늘 미션을 활성 관리자 미션 중에서
     * 중복 없이 무작위로 추가 배정합니다.
     */
    private void assignMissingMissions(
            User user,
            LocalDate assignedDate,
            List<Mission> assignedMissions
    ) {

        List<MissionDefinition> activeDefinitions =
                new ArrayList<>(
                        missionDefinitionRepository
                                .findByScopeAndActiveTrueOrderByIdAsc(
                                        MissionScope.PERSONAL
                                )
                );

        if (activeDefinitions.size()
                < DAILY_MISSION_COUNT) {

            throw new BusinessException(
                    "활성화된 일일 미션이 최소 3개 필요합니다."
            );
        }

        /*
         * 오늘 이미 배정된 관리자 미션 ID를 수집합니다.
         */
        Set<Long> assignedDefinitionIds =
                new HashSet<>();

        for (Mission mission : assignedMissions) {

            if (mission.getDefinition() != null) {
                assignedDefinitionIds.add(
                        mission.getDefinition().getId()
                );
            }
        }

        /*
         * 오늘 이미 배정된 미션은 후보에서 제외합니다.
         */
        activeDefinitions.removeIf(definition ->
                assignedDefinitionIds.contains(
                        definition.getId()
                )
        );

        Collections.shuffle(
                activeDefinitions,
                random
        );

        int missingCount =
                DAILY_MISSION_COUNT
                        - assignedMissions.size();

        if (activeDefinitions.size() < missingCount) {
            throw new BusinessException(
                    "중복되지 않는 활성 일일 미션이 부족합니다."
            );
        }

        for (int index = 0;
             index < missingCount;
             index++) {

            MissionDefinition definition =
                    activeDefinitions.get(index);

            Mission mission =
                    Mission.fromDefinition(
                            user,
                            definition,
                            assignedDate,
                            definition.rollTarget(random)
                    );

            Mission savedMission =
                    missionRepository.save(mission);

            assignedMissions.add(savedMission);
        }
    }

    /**
     * 해당 날짜의 사용자 미션 진행도를 조회합니다.
     *
     * 없으면 새로운 진행도 데이터를 생성합니다.
     */
    private MissionCounter getOrCreateCounter(
            Long userId,
            LocalDate date,
            String exerciseType
    ) {

        String normalizedExerciseType =
                normalizeExerciseType(exerciseType);

        return counterRepository
                .findByUserIdAndCounterDateAndExerciseType(
                        userId,
                        date,
                        normalizedExerciseType
                )
                .orElseGet(() ->
                        counterRepository.save(
                                MissionCounter.startFor(
                                        userRepository
                                                .getReferenceById(
                                                        userId
                                                ),
                                        date,
                                        normalizedExerciseType
                                )
                        )
                );
    }

    private String normalizeExerciseType(String exerciseType) {

        if (exerciseType == null || exerciseType.isBlank()) {
            throw new BusinessException("운동 종류가 필요합니다.");
        }

        String normalized =
                exerciseType.trim().toUpperCase(Locale.ROOT);

        if ("스쿼트".equals(normalized)) {
            return "SQUAT";
        }

        return normalized;
    }
}
