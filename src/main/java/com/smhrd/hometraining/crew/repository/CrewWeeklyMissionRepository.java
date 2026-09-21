package com.smhrd.hometraining.crew.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smhrd.hometraining.crew.entity.CrewWeeklyMission;

import jakarta.persistence.LockModeType;

// [DB 접근 지점] crew_weekly_missions 테이블(크루 단위 주간 공동 미션).
public interface CrewWeeklyMissionRepository
        extends JpaRepository<CrewWeeklyMission, Long> {

    /**
     * 특정 크루의 특정 주차 미션을 조회합니다.
     */
    Optional<CrewWeeklyMission> findByCrewIdAndWeekStart(
            Long crewId,
            LocalDate weekStart
    );

    /**
     * 주간 미션 진행도와 보상 지급을 변경할 때
     * 중복 처리를 막기 위해 DB 행을 잠급니다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT mission
            FROM CrewWeeklyMission mission
            WHERE mission.crew.id = :crewId
              AND mission.weekStart = :weekStart
            """)
    Optional<CrewWeeklyMission>
            findByCrewIdAndWeekStartForUpdate(
                    @Param("crewId")
                    Long crewId,

                    @Param("weekStart")
                    LocalDate weekStart
            );

    void deleteByCrewId(Long crewId);
}