package com.smhrd.hometraining.crew.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smhrd.hometraining.crew.entity.CrewExperienceHistory;
import com.smhrd.hometraining.crew.entity.CrewExperienceHistory.SourceType;

public interface CrewExperienceHistoryRepository
        extends JpaRepository<CrewExperienceHistory, Long> {

    /**
     * 같은 원인으로 이미 경험치를 지급했는지 확인합니다.
     *
     * 주간 미션이나 크루대전 보상 중복 지급을
     * 방지할 때 사용합니다.
     */
    boolean existsByCrewIdAndSourceTypeAndSourceId(
            Long crewId,
            SourceType sourceType,
            Long sourceId
    );

    /**
     * 크루 경험치 변경 이력을 최신순으로 조회합니다.
     */
    List<CrewExperienceHistory>
            findByCrewIdOrderByCreatedAtDesc(
                    Long crewId
            );

    /**
     * 특정 주차에 크루대전으로 지급된
     * 경험치 총합을 계산합니다.
     *
     * 주간 최대 500 EXP 제한에 사용합니다.
     */
    @Query("""
            SELECT COALESCE(
                    SUM(history.expAmount),
                    0
            )
            FROM CrewExperienceHistory history
            WHERE history.crew.id = :crewId
              AND history.weekStart = :weekStart
              AND history.sourceType IN :sourceTypes
            """)
    long sumExpByCrewIdAndWeekStartAndSourceTypes(
            @Param("crewId")
            Long crewId,

            @Param("weekStart")
            LocalDate weekStart,

            @Param("sourceTypes")
            Collection<SourceType> sourceTypes
    );
}