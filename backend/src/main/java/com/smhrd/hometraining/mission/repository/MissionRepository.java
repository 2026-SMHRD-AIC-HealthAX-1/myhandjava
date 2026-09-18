package com.smhrd.hometraining.mission.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.smhrd.hometraining.mission.entity.Mission;

import jakarta.persistence.LockModeType;

public interface MissionRepository
        extends JpaRepository<Mission, Long> {

    /**
     * 사용자의 특정 날짜 미션을
     * 등록 순서대로 조회합니다.
     */
    List<Mission> findByUserIdAndAssignedDateOrderByIdAsc(
            Long userId,
            LocalDate assignedDate
    );

    /**
     * 보상 중복 수령을 방지하기 위해
     * 미션 DB 행을 쓰기 잠금 상태로 조회합니다.
     *
     * 미션 ID와 사용자 ID를 함께 확인하므로
     * 다른 사용자의 미션 보상은 받을 수 없습니다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Mission> findByIdAndUserId(
            Long missionId,
            Long userId
    );
}