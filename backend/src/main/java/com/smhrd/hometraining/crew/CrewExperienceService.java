package com.smhrd.hometraining.crew;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smhrd.hometraining.common.exception.BusinessException;
import com.smhrd.hometraining.crew.dto.CrewExperienceHistoryResponse;
import com.smhrd.hometraining.crew.entity.Crew;
import com.smhrd.hometraining.crew.entity.CrewExperienceHistory;
import com.smhrd.hometraining.crew.entity.CrewExperienceHistory.SourceType;
import com.smhrd.hometraining.crew.entity.CrewMember;
import com.smhrd.hometraining.crew.policy.CrewExperiencePolicy;
import com.smhrd.hometraining.crew.policy.CrewWeeklyMissionPolicy;
import com.smhrd.hometraining.crew.repository.CrewExperienceHistoryRepository;
import com.smhrd.hometraining.crew.repository.CrewMemberRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CrewExperienceService {

    private static final List<SourceType> BATTLE_SOURCE_TYPES =
            List.of(
                    SourceType.BATTLE_WIN,
                    SourceType.BATTLE_LOSS,
                    SourceType.BATTLE_DRAW
            );

    private final CrewExperienceHistoryRepository
            crewExperienceHistoryRepository;

    private final CrewMemberRepository
            crewMemberRepository;

    private final EntityManager entityManager;

    /**
     * 로그인한 사용자가 소속된 크루의
     * 경험치 지급 내역을 최신순으로 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<CrewExperienceHistoryResponse> getMyCrewHistory(
            Long userId
    ) {

        CrewMember member =
                crewMemberRepository
                        .findByUserId(userId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "소속된 크루가 없습니다."
                                )
                        );

        return crewExperienceHistoryRepository
                .findByCrewIdOrderByCreatedAtDesc(
                        member.getCrew().getId()
                )
                .stream()
                .map(CrewExperienceHistoryResponse::from)
                .toList();
    }

    /**
     * 크루 주간 미션 완료 경험치 500을 지급합니다.
     *
     * 같은 주간 미션 ID로는 한 번만 지급합니다.
     */
    @Transactional
    public int grantWeeklyMissionReward(
            Long crewId,
            Long weeklyMissionId
    ) {

        if (weeklyMissionId == null) {
            throw new BusinessException(
                    "주간 미션 ID가 필요합니다."
            );
        }

        Crew crew =
                getCrewForUpdate(crewId);

        boolean alreadyGranted =
                crewExperienceHistoryRepository
                        .existsByCrewIdAndSourceTypeAndSourceId(
                                crewId,
                                SourceType.WEEKLY_MISSION,
                                weeklyMissionId
                        );

        if (alreadyGranted) {
            return 0;
        }

        LocalDate weekStart =
                CrewWeeklyMissionPolicy
                        .getCurrentWeekStart();

        return applyExperience(
                crew,
                SourceType.WEEKLY_MISSION,
                weeklyMissionId,
                CrewExperiencePolicy
                        .WEEKLY_MISSION_REWARD_EXP,
                weekStart
        );
    }

    /**
     * 크루대전 결과에 따라 경험치를 지급합니다.
     *
     * 승리 100 EXP
     * 패배 50 EXP
     * 무승부 0 EXP
     *
     * 크루대전으로 획득할 수 있는 경험치는
     * 한 주에 최대 500 EXP입니다.
     */
    @Transactional
    public int grantBattleReward(
            Long crewId,
            Long battleId,
            SourceType sourceType
    ) {

        if (battleId == null) {
            throw new BusinessException(
                    "크루대전 ID가 필요합니다."
            );
        }

        validateBattleSourceType(sourceType);

        Crew crew =
                getCrewForUpdate(crewId);

        boolean alreadyGranted =
                crewExperienceHistoryRepository
                        .existsByCrewIdAndSourceTypeAndSourceId(
                                crewId,
                                sourceType,
                                battleId
                        );

        if (alreadyGranted) {
            return 0;
        }

        LocalDate weekStart =
                CrewWeeklyMissionPolicy
                        .getCurrentWeekStart();

        long awardedThisWeekLong =
                crewExperienceHistoryRepository
                        .sumExpByCrewIdAndWeekStartAndSourceTypes(
                                crewId,
                                weekStart,
                                BATTLE_SOURCE_TYPES
                        );

        int awardedThisWeek =
                awardedThisWeekLong > Integer.MAX_VALUE
                        ? Integer.MAX_VALUE
                        : (int) awardedThisWeekLong;

        int requestedExp =
                getBattleRequestedExp(
                        sourceType
                );

        int actualExp =
                CrewExperiencePolicy
                        .calculateBattleExp(
                                requestedExp,
                                awardedThisWeek
                        );

        /*
         * 실제 지급량이 0이어도 이력을 저장해
         * 같은 대전 결과가 반복 처리되지 않게 합니다.
         */
        return applyExperience(
                crew,
                sourceType,
                battleId,
                actualExp,
                weekStart
        );
    }

    /**
     * 경험치를 적용하고 레벨업한 뒤
     * 변경 이력을 저장합니다.
     */
    private int applyExperience(
            Crew crew,
            SourceType sourceType,
            Long sourceId,
            int expAmount,
            LocalDate weekStart
    ) {

        int levelBefore =
                crew.getLevel();

        int expBefore =
                crew.getExp();

        int safeExpAmount =
                Math.max(expAmount, 0);

        int totalExp =
                expBefore + safeExpAmount;

        int levelUps =
                totalExp
                        / CrewExperiencePolicy.EXP_PER_LEVEL;

        int levelAfter =
                levelBefore + levelUps;

        int expAfter =
                totalExp
                        % CrewExperiencePolicy.EXP_PER_LEVEL;

        crew.setLevel(levelAfter);
        crew.setExp(expAfter);

        /*
         * 실제 경험치가 지급된 경우에만
         * 현재 레벨·경험치 달성 시간을 갱신합니다.
         *
         * 무승부 또는 주간 한도 초과로 지급 경험치가 0이면
         * 기존 달성 시간을 변경하지 않습니다.
         */
        if (safeExpAmount > 0) {
            crew.markRankingAchievedNow();
        }

        CrewExperienceHistory history =
                CrewExperienceHistory.create(
                        crew,
                        sourceType,
                        sourceId,
                        safeExpAmount,
                        levelBefore,
                        levelAfter,
                        expBefore,
                        expAfter,
                        weekStart
                );

        crewExperienceHistoryRepository.save(
                history
        );

        return safeExpAmount;
    }

    /**
     * 크루를 DB 잠금 상태로 조회합니다.
     */
    private Crew getCrewForUpdate(
            Long crewId
    ) {

        if (crewId == null) {
            throw new BusinessException(
                    "크루 ID가 필요합니다."
            );
        }

        Crew crew =
                entityManager.find(
                        Crew.class,
                        crewId,
                        LockModeType.PESSIMISTIC_WRITE
                );

        if (crew == null) {
            throw new BusinessException(
                    "크루를 찾을 수 없습니다."
            );
        }

        return crew;
    }

    /**
     * 크루대전 경험치 지급 사유인지 검사합니다.
     */
    private void validateBattleSourceType(
            SourceType sourceType
    ) {

        if (!BATTLE_SOURCE_TYPES.contains(
                sourceType
        )) {

            throw new BusinessException(
                    "올바른 크루대전 결과가 아닙니다."
            );
        }
    }

    /**
     * 크루대전 결과별 기본 경험치를 반환합니다.
     */
    private int getBattleRequestedExp(
            SourceType sourceType
    ) {

        return switch (sourceType) {

            case BATTLE_WIN ->
                    CrewExperiencePolicy.BATTLE_WIN_EXP;

            case BATTLE_LOSS ->
                    CrewExperiencePolicy.BATTLE_LOSS_EXP;

            case BATTLE_DRAW ->
                    CrewExperiencePolicy.BATTLE_DRAW_EXP;

            default ->
                    throw new BusinessException(
                            "올바른 크루대전 결과가 아닙니다."
                    );
        };
    }
}