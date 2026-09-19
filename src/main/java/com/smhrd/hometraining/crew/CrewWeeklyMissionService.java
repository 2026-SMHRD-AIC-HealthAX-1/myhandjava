package com.smhrd.hometraining.crew;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smhrd.hometraining.common.exception.BusinessException;
import com.smhrd.hometraining.crew.dto.CrewWeeklyMissionResponse;
import com.smhrd.hometraining.crew.entity.Crew;
import com.smhrd.hometraining.crew.entity.CrewMember;
import com.smhrd.hometraining.crew.entity.CrewWeeklyContribution;
import com.smhrd.hometraining.crew.entity.CrewWeeklyMission;
import com.smhrd.hometraining.crew.policy.CrewWeeklyMissionPolicy;
import com.smhrd.hometraining.crew.repository.CrewMemberRepository;
import com.smhrd.hometraining.crew.repository.CrewWeeklyContributionRepository;
import com.smhrd.hometraining.crew.repository.CrewWeeklyMissionRepository;
import com.smhrd.hometraining.exercise.repository.ExerciseRecordRepository;
import com.smhrd.hometraining.mission.entity.MissionDefinition;
import com.smhrd.hometraining.mission.entity.MissionScope;
import com.smhrd.hometraining.mission.repository.MissionDefinitionRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CrewWeeklyMissionService {

    private final SecureRandom random =
            new SecureRandom();

    private final CrewMemberRepository
            crewMemberRepository;

    private final CrewWeeklyMissionRepository
            crewWeeklyMissionRepository;

    private final CrewWeeklyContributionRepository
            crewWeeklyContributionRepository;

    private final ExerciseRecordRepository
            exerciseRecordRepository;

    private final MissionDefinitionRepository
            missionDefinitionRepository;

    private final CrewExperienceService
            crewExperienceService;

    private final EntityManager entityManager;

    /**
     * 로그인한 사용자가 소속된 크루의
     * 현재 주간 미션 현황을 조회합니다.
     *
     * 이번 주 미션이 없으면 자동으로 생성하고,
     * 운동 기록을 다시 계산해 진행도를 갱신합니다.
     */
    @Transactional
    public CrewWeeklyMissionResponse getCurrentMission(
            Long userId
    ) {

        CrewMember requester =
                crewMemberRepository
                        .findByUserId(userId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "소속된 크루가 없습니다."
                                )
                        );

        Crew crew =
                requester.getCrew();

        /*
         * 같은 크루에서 여러 명이 동시에 조회해도
         * 주간 미션과 보상이 중복 생성되지 않도록 잠급니다.
         */
        entityManager.lock(
                crew,
                LockModeType.PESSIMISTIC_WRITE
        );

        LocalDate weekStart =
                CrewWeeklyMissionPolicy
                        .getCurrentWeekStart();

        CrewWeeklyMission weeklyMission =
                getOrCreateWeeklyMission(
                        crew,
                        weekStart
                );

        List<CrewMember> crewMembers =
                crewMemberRepository
                        .findByCrewIdOrderByRoleAscJoinedAtAsc(
                                crew.getId()
                        );

        LocalDateTime from =
                weekStart.atStartOfDay();

        LocalDateTime to =
                weekStart
                        .plusWeeks(1)
                        .atStartOfDay();

        List<CrewWeeklyContribution> contributions =
                new ArrayList<>();

        int totalRecognizedReps = 0;

        for (CrewMember crewMember : crewMembers) {

            Long memberUserId =
                    crewMember.getUser().getId();

            long calculatedReps =
                    exerciseRecordRepository
                            .sumRepsByUserIdAndExerciseTypeAndPeriod(
                                    memberUserId,
                                    weeklyMission.getExerciseType(),
                                    from,
                                    to,
                                    CrewWeeklyMissionPolicy
                                            .INCLUDE_RETAKE_TICKET_REPS
                            );

            int safeCalculatedReps =
                    calculatedReps > Integer.MAX_VALUE
                            ? Integer.MAX_VALUE
                            : (int) calculatedReps;

            CrewWeeklyContribution contribution =
                    getOrCreateContribution(
                            weeklyMission,
                            crewMember
                    );

            contribution.updateTotalReps(
                    safeCalculatedReps
            );

            totalRecognizedReps +=
                    contribution.getRecognizedReps();

            contributions.add(contribution);
        }

        weeklyMission.updateProgress(
                totalRecognizedReps
        );

        /*
         * 목표 300회를 처음 달성한 요청에서만
         * CrewExperienceService를 통해
         * 크루 경험치 500을 지급합니다.
         *
         * 경험치 지급과 동시에 레벨업 처리 및
         * 경험치 변경 이력이 저장됩니다.
         */
        if (weeklyMission.isCompleted()
                && !weeklyMission.isRewardGranted()) {

            crewExperienceService
                    .grantWeeklyMissionReward(
                            crew.getId(),
                            weeklyMission.getId()
                    );

            weeklyMission.markRewardGranted();
        }

        contributions.sort(
                Comparator
                        .comparingInt(
                                CrewWeeklyContribution
                                        ::getRecognizedReps
                        )
                        .reversed()
        );

        return CrewWeeklyMissionResponse.of(
                weeklyMission,
                contributions
        );
    }

    /**
     * 이번 주의 크루 주간 미션을 조회하고,
     * 없으면 새로 생성합니다.
     */
    private CrewWeeklyMission getOrCreateWeeklyMission(
            Crew crew,
            LocalDate weekStart
    ) {

        return crewWeeklyMissionRepository
                .findByCrewIdAndWeekStartForUpdate(
                        crew.getId(),
                        weekStart
                )
                .orElseGet(() -> {
                    List<MissionDefinition> definitions =
                            missionDefinitionRepository
                                    .findByScopeAndActiveTrueOrderByIdAsc(
                                            MissionScope.CREW
                                    );

                    if (definitions.isEmpty()) {
                        throw new BusinessException(
                                "활성화된 크루 미션이 없습니다."
                        );
                    }

                    MissionDefinition definition =
                            definitions.get(
                                    random.nextInt(definitions.size())
                            );

                    return crewWeeklyMissionRepository
                            .saveAndFlush(
                                    CrewWeeklyMission.startFor(
                                            crew,
                                            definition,
                                            definition.rollTarget(random),
                                            weekStart
                                    )
                            );
                });
    }

    /**
     * 크루원별 이번 주 기여도를 조회하고,
     * 없으면 새로 생성합니다.
     */
    private CrewWeeklyContribution getOrCreateContribution(
            CrewWeeklyMission weeklyMission,
            CrewMember crewMember
    ) {

        Long userId =
                crewMember.getUser().getId();

        return crewWeeklyContributionRepository
                .findByWeeklyMissionIdAndUserIdForUpdate(
                        weeklyMission.getId(),
                        userId
                )
                .orElseGet(() ->
                        crewWeeklyContributionRepository
                                .save(
                                        CrewWeeklyContribution
                                                .startFor(
                                                        weeklyMission,
                                                        crewMember.getUser()
                                                )
                                )
                );
    }
}
