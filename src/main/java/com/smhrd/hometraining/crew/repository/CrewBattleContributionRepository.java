package com.smhrd.hometraining.crew.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smhrd.hometraining.crew.entity.CrewBattleContribution;

import jakarta.persistence.LockModeType;

public interface CrewBattleContributionRepository
        extends JpaRepository<CrewBattleContribution, Long> {

    List<CrewBattleContribution>
            findByCrewIdOrderByTotalScoreDescUpdatedAtAsc(
                    Long crewId
            );

    Optional<CrewBattleContribution>
            findByCrewIdAndUserId(
                    Long crewId,
                    Long userId
            );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT contribution
            FROM CrewBattleContribution contribution
            WHERE contribution.crew.id = :crewId
              AND contribution.user.id = :userId
            """)
    Optional<CrewBattleContribution>
            findByCrewIdAndUserIdForUpdate(
                    @Param("crewId") Long crewId,
                    @Param("userId") Long userId
            );

    void deleteByCrewIdAndUserId(
            Long crewId,
            Long userId
    );

    void deleteByCrewId(Long crewId);

    void deleteByUserId(Long userId);
}
