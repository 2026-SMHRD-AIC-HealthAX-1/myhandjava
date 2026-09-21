package com.smhrd.hometraining.mission.repository;

import com.smhrd.hometraining.mission.entity.MissionCounter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

// [DB 접근 지점] mission_counters 테이블(날짜·운동종류별 누적 카운터, 미션 달성 판정 기준).
public interface MissionCounterRepository extends JpaRepository<MissionCounter, Long> {
    Optional<MissionCounter> findByUserIdAndCounterDateAndExerciseType(
            Long userId,
            LocalDate date,
            String exerciseType
    );

    void deleteByUserId(Long userId);
}
