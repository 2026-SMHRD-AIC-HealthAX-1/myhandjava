package com.smhrd.hometraining.mission.repository;

import com.smhrd.hometraining.mission.entity.Mission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MissionRepository extends JpaRepository<Mission, Long> {
    List<Mission> findByUserIdAndAssignedDate(Long userId, LocalDate date);
}
