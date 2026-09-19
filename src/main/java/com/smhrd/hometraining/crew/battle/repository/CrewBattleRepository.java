package com.smhrd.hometraining.crew.battle.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smhrd.hometraining.crew.battle.entity.CrewBattle;

import jakarta.persistence.LockModeType;

public interface CrewBattleRepository
        extends JpaRepository<CrewBattle, Long> {

    /**
     * 크루가 이미 대기, 매칭 완료 또는 진행 중인
     * 대전을 가지고 있는지 확인합니다.
     */
    @Query("""
            SELECT (COUNT(battle) > 0)
            FROM CrewBattle battle
            WHERE battle.status IN :statuses
              AND (
                    battle.challenger.id = :crewId
                    OR battle.opponent.id = :crewId
              )
            """)
    boolean existsOpenBattle(
            @Param("crewId")
            Long crewId,

            @Param("statuses")
            Collection<CrewBattle.Status> statuses
    );

    /**
     * 특정 크루가 참가한 모든 대전을
     * 최근 생성된 순서로 조회합니다.
     */
    @Query("""
            SELECT battle
            FROM CrewBattle battle
            WHERE battle.challenger.id = :crewId
               OR battle.opponent.id = :crewId
            ORDER BY battle.createdAt DESC,
                     battle.id DESC
            """)
    List<CrewBattle> findMine(
            @Param("crewId")
            Long crewId
    );

    /**
     * 대전 상태를 변경할 때 동일 대전에 여러 요청이
     * 동시에 들어오는 것을 막기 위한 잠금 조회입니다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT battle
            FROM CrewBattle battle
            WHERE battle.id = :battleId
            """)
    Optional<CrewBattle> findByIdForUpdate(
            @Param("battleId")
            Long battleId
    );

    /**
     * 같은 팀 크기와 운동 종류로 대기 중인
     * 상대 크루 후보를 오래 기다린 순서로 조회합니다.
     *
     * 자기 크루와 직전 상대 크루는 조회에서 제외합니다.
     * Pageable을 사용해 조회할 후보 개수를 제한합니다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT battle
            FROM CrewBattle battle
            WHERE battle.status = :waitingStatus
              AND battle.challenger.id <> :requesterCrewId
              AND SIZE(battle.challengerUserIds) = :teamSize
              AND battle.exerciseType = :exerciseType
              AND (
                    :excludedCrewId IS NULL
                    OR battle.challenger.id <> :excludedCrewId
              )
            ORDER BY battle.createdAt ASC,
                     battle.id ASC
            """)
    List<CrewBattle> findWaitingCandidatesForUpdate(
            @Param("requesterCrewId")
            Long requesterCrewId,

            @Param("teamSize")
            int teamSize,

            @Param("exerciseType")
            String exerciseType,

            @Param("excludedCrewId")
            Long excludedCrewId,

            @Param("waitingStatus")
            CrewBattle.Status waitingStatus,

            Pageable pageable
    );

    /**
     * 특정 크루가 가장 최근에 종료한 대전을 조회합니다.
     *
     * 첫 번째 결과의 상대 크루가 직전 대전 상대입니다.
     */
    @Query("""
            SELECT battle
            FROM CrewBattle battle
            WHERE battle.status = :finishedStatus
              AND (
                    battle.challenger.id = :crewId
                    OR battle.opponent.id = :crewId
              )
            ORDER BY COALESCE(
                         battle.finishedAt,
                         battle.createdAt
                     ) DESC,
                     battle.id DESC
            """)
    List<CrewBattle> findLatestFinishedBattles(
            @Param("crewId")
            Long crewId,

            @Param("finishedStatus")
            CrewBattle.Status finishedStatus,

            Pageable pageable
    );
}