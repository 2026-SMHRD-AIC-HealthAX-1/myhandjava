package com.smhrd.hometraining.mission.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smhrd.hometraining.mission.entity.MissionDefinition;
import com.smhrd.hometraining.mission.entity.MissionMetric;
import com.smhrd.hometraining.mission.entity.MissionScope;

public interface MissionDefinitionRepository
        extends JpaRepository<MissionDefinition, Long> {

    /**
     * 관리자가 등록한 전체 미션을
     * 등록 순서대로 조회합니다.
     */
    List<MissionDefinition> findAllByOrderByIdAsc();

    /**
     * 활성화된 미션만 등록 순서대로 조회합니다.
     *
     * 사용자에게 일일 미션을 배정할 때 사용합니다.
     */
    List<MissionDefinition> findByScopeAndActiveTrueOrderByIdAsc(
            MissionScope scope
    );

    /**
     * 동일한 미션 종류, 운동 종류, 목표값을 가진
     * 미션이 이미 등록되어 있는지 확인합니다.
     *
     * 신규 미션 등록 시 사용합니다.
     */
    boolean existsByScopeAndMetricAndExerciseTypeAndMinTargetAndMaxTarget(
            MissionScope scope,
            MissionMetric metric,
            String exerciseType,
            int minTarget,
            int maxTarget
    );

    /**
     * 현재 수정 중인 미션 ID는 제외하고
     * 동일한 미션이 존재하는지 확인합니다.
     *
     * 기존 미션 수정 시 사용합니다.
     */
    boolean existsByScopeAndMetricAndExerciseTypeAndMinTargetAndMaxTargetAndIdNot(
            MissionScope scope,
            MissionMetric metric,
            String exerciseType,
            int minTarget,
            int maxTarget,
            Long id
    );
}
