package com.smhrd.hometraining.crew.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smhrd.hometraining.crew.entity.CrewWeeklyContribution;

import jakarta.persistence.LockModeType;

public interface CrewWeeklyContributionRepository
        extends JpaRepository<CrewWeeklyContribution, Long> {

    /**
     * 주간 미션에 참여한 크루원들의 기여도를
     * 인정 횟수 내림차순으로 조회합니다.
     */
    List<CrewWeeklyContribution>
            findByWeeklyMissionIdOrderByRecognizedRepsDesc(
                    Long weeklyMissionId
            );

    /**
     * 특정 주간 미션에서 특정 사용자의
     * 기여도 정보를 조회합니다.
     */
    Optional<CrewWeeklyContribution>
            findByWeeklyMissionIdAndUserId(
                    Long weeklyMissionId,
                    Long userId
            );

    /**
     * 기여도 변경 시 같은 사용자의 운동 결과가
     * 동시에 반영되는 것을 막기 위해 DB 행을 잠급니다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT contribution
            FROM CrewWeeklyContribution contribution
            WHERE contribution.weeklyMission.id = :weeklyMissionId
              AND contribution.user.id = :userId
            """)
    Optional<CrewWeeklyContribution>
            findByWeeklyMissionIdAndUserIdForUpdate(
                    @Param("weeklyMissionId")
                    Long weeklyMissionId,

                    @Param("userId")
                    Long userId
            );

    /**
     * 크루원별 최대 80회 기준이 적용된
     * 인정 횟수를 모두 합산합니다.
     */
    @Query("""
            SELECT COALESCE(
                    SUM(contribution.recognizedReps),
                    0
            )
            FROM CrewWeeklyContribution contribution
            WHERE contribution.weeklyMission.id = :weeklyMissionId
            """)
    long sumRecognizedRepsByWeeklyMissionId(
            @Param("weeklyMissionId")
            Long weeklyMissionId
    );

    void deleteByUserId(Long userId);
}