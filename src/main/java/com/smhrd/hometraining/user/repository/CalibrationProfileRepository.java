package com.smhrd.hometraining.user.repository;

import com.smhrd.hometraining.user.entity.CalibrationProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// [DB 접근 지점] calibration_profiles 테이블(카메라 체형 보정 결과 JSON).
public interface CalibrationProfileRepository extends JpaRepository<CalibrationProfile, Long> {
    Optional<CalibrationProfile> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}
