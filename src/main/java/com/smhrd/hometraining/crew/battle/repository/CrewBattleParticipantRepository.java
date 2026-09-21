package com.smhrd.hometraining.crew.battle.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smhrd.hometraining.crew.battle.entity.CrewBattleParticipant;

import jakarta.persistence.LockModeType;

// [DB 접근 지점] crew_battle_participants 테이블.
public interface CrewBattleParticipantRepository
        extends JpaRepository<CrewBattleParticipant, Long> {

    /**
     * 특정 대전에 참여한 전체 참가자를 조회합니다.
     */
    List<CrewBattleParticipant> findByBattle_Id(
            Long battleId
    );

    /**
     * 특정 대전에서 한쪽 크루의 참가자를 조회합니다.
     */
    List<CrewBattleParticipant> findByBattle_IdAndCrew_Id(
            Long battleId,
            Long crewId
    );

    /**
     * 특정 대전에서 특정 사용자를 조회합니다.
     */
    Optional<CrewBattleParticipant> findByBattle_IdAndUser_Id(
            Long battleId,
            Long userId
    );

    /**
     * 특정 크루의 실제 대전 참가 인원수를 조회합니다.
     */
    long countByBattle_IdAndCrew_Id(
            Long battleId,
            Long crewId
    );

    /**
     * 동시에 운동 판정이 들어올 때 충돌하지 않도록
     * 참가자 데이터를 DB 잠금 상태로 조회합니다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT participant
            FROM CrewBattleParticipant participant
            WHERE participant.battle.id = :battleId
              AND participant.user.id = :userId
            """)
    Optional<CrewBattleParticipant> findForUpdate(
            @Param("battleId")
            Long battleId,

            @Param("userId")
            Long userId
    );

    /**
     * 특정 대전에서 한쪽 크루 참가자들의
     * 인정된 운동 횟수를 모두 합산합니다.
     *
     * MISS 횟수는 포함하지 않습니다.
     */
    @Query("""
            SELECT COALESCE(SUM(participant.validCount), 0)
            FROM CrewBattleParticipant participant
            WHERE participant.battle.id = :battleId
              AND participant.crew.id = :crewId
            """)
    long sumValidCount(
            @Param("battleId")
            Long battleId,

            @Param("crewId")
            Long crewId
    );

    /**
     * 특정 대전에서 한쪽 크루 참가자들의
     * 전체 점수를 모두 합산합니다.
     *
     * 대전 승패는 이 점수를 기준으로 결정합니다.
     */
    @Query("""
            SELECT COALESCE(SUM(participant.totalScore), 0)
            FROM CrewBattleParticipant participant
            WHERE participant.battle.id = :battleId
              AND participant.crew.id = :crewId
            """)
    long sumTotalScore(
            @Param("battleId")
            Long battleId,

            @Param("crewId")
            Long crewId
    );

    /**
     * 특정 대전에서 한쪽 크루 참가자들의
     * 전체 시도 횟수를 계산합니다.
     *
     * PERFECT, GREAT, GOOD, MISS를 모두 포함합니다.
     */
    @Query("""
            SELECT COALESCE(
                SUM(
                    participant.perfectCount
                    + participant.greatCount
                    + participant.goodCount
                    + participant.missCount
                ),
                0
            )
            FROM CrewBattleParticipant participant
            WHERE participant.battle.id = :battleId
              AND participant.crew.id = :crewId
            """)
    long sumTotalAttempts(
            @Param("battleId")
            Long battleId,

            @Param("crewId")
            Long crewId
    );

    void deleteByUser_Id(Long userId);

    void deleteByBattle_Id(Long battleId);
}