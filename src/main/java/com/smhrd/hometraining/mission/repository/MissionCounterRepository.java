package com.smhrd.hometraining.mission.repository;

import com.smhrd.hometraining.mission.entity.MissionCounter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface MissionCounterRepository extends JpaRepository<MissionCounter, Long> {
    Optional<MissionCounter> findByUserIdAndCounterDateAndExerciseType(
            Long userId,
            LocalDate date,
            String exerciseType
    );

    void deleteByUserId(Long userId);
}
