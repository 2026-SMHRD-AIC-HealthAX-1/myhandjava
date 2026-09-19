package com.smhrd.hometraining.user.repository;

import com.smhrd.hometraining.user.entity.CalibrationProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CalibrationProfileRepository extends JpaRepository<CalibrationProfile, Long> {
    Optional<CalibrationProfile> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}
