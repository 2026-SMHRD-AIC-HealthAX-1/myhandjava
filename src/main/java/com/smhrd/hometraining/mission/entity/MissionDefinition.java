package com.smhrd.hometraining.mission.entity;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.random.RandomGenerator;

import com.smhrd.hometraining.common.exception.BusinessException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자가 등록하는 개인 일일 미션 원본입니다.
 *
 * 실제 사용자에게 배정된 미션은 Mission 엔티티에 저장하고,
 * 이 엔티티는 미션 배정에 사용할 원본 목록을 관리합니다.
 */
@Entity
@Table(name = "mission_definitions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MissionDefinition {

    public static final int DEFAULT_REWARD_POINTS = 50;
    public static final int DEFAULT_REWARD_EXP = 50;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MissionScope scope;

    /**
     * 미션 진행 조건입니다.
     *
     * REPS, PERFECT, SESSIONS,
     * MISS_FREE_SESSION, ACC_SESSION 중 하나입니다.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MissionMetric metric;

    /**
     * 미션에 적용되는 운동 종류입니다.
     *
     * 현재는 SQUAT을 기본으로 사용합니다.
     */
    @Column(name = "exercise_type", nullable = false, length = 30)
    private String exerciseType;

    /**
     * 미션 완료에 필요한 목표 횟수입니다.
     */
    @Column(name = "min_target", nullable = false)
    private int minTarget;

    @Column(name = "max_target", nullable = false)
    private int maxTarget;

    /**
     * 프론트엔드에 표시할 미션 이름입니다.
     */
    @Column(nullable = false, length = 100)
    private String label;

    /**
     * 미션 완료 후 지급할 포인트입니다.
     */
    @Column(name = "reward_points", nullable = false)
    private int rewardPoints = DEFAULT_REWARD_POINTS;

    /**
     * 미션 완료 후 지급할 경험치입니다.
     */
    @Column(name = "reward_exp", nullable = false)
    private int rewardExp = DEFAULT_REWARD_EXP;

    /**
     * 미션 배정에 사용할 수 있는지 나타냅니다.
     *
     * false이면 기존 데이터는 유지하지만
     * 신규 사용자 미션 배정에는 사용하지 않습니다.
     */
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 새로운 관리자 미션 원본을 생성합니다.
     */
    public static MissionDefinition create(
            MissionScope scope,
            MissionMetric metric,
            String exerciseType,
            int minTarget,
            int maxTarget,
            String label,
            int rewardPoints,
            int rewardExp
    ) {

        MissionDefinition definition =
                new MissionDefinition();

        definition.changeMissionInformation(
                scope,
                metric,
                exerciseType,
                minTarget,
                maxTarget,
                label,
                rewardPoints,
                rewardExp
        );

        definition.active = true;

        return definition;
    }

    /**
     * 기존 관리자 미션 원본의 내용을 수정합니다.
     */
    public void update(
            MissionScope scope,
            MissionMetric metric,
            String exerciseType,
            int minTarget,
            int maxTarget,
            String label,
            int rewardPoints,
            int rewardExp
    ) {

        changeMissionInformation(
                scope,
                metric,
                exerciseType,
                minTarget,
                maxTarget,
                label,
                rewardPoints,
                rewardExp
        );
    }

    /**
     * 미션을 신규 배정에 사용할 수 있도록 활성화합니다.
     */
    public void activate() {
        this.active = true;
    }

    /**
     * 미션을 신규 배정에서 제외하도록 비활성화합니다.
     *
     * 데이터는 삭제하지 않습니다.
     */
    public void deactivate() {
        this.active = false;
    }

    /**
     * 미션 종류, 운동 종류, 목표값과 표시 문구를
     * 공통으로 검증하고 변경합니다.
     */
    private void changeMissionInformation(
            MissionScope scope,
            MissionMetric metric,
            String exerciseType,
            int minTarget,
            int maxTarget,
            String label,
            int rewardPoints,
            int rewardExp
    ) {

        if (scope == null) {
            throw new BusinessException("Mission scope is required.");
        }

        if (metric == null) {
            throw new BusinessException(
                    "미션 종류가 필요합니다."
            );
        }

        if (scope == MissionScope.PERSONAL) {
            metric.validateTarget(minTarget);
            metric.validateTarget(maxTarget);
        } else if (minTarget < 1 || maxTarget < 1) {
            throw new BusinessException(
                    "Crew mission targets must be at least 1."
            );
        }

        if (scope == MissionScope.CREW
                && metric != MissionMetric.REPS) {
            throw new BusinessException(
                    "Crew missions currently support the REPS condition only."
            );
        }

        if (minTarget > maxTarget) {
            throw new BusinessException(
                    "Minimum target cannot exceed maximum target."
            );
        }

        if (rewardPoints < 0 || rewardExp < 0) {
            throw new BusinessException(
                    "Mission rewards cannot be negative."
            );
        }

        this.scope = scope;
        this.metric = metric;
        this.exerciseType =
                normalizeExerciseType(exerciseType);

        if (label == null || label.isBlank()) {
            this.label = metric.label(minTarget);
        } else {
            String trimmedLabel = label.trim();

            if (trimmedLabel.length() > 100) {
                throw new BusinessException(
                        "미션 이름은 100자 이하여야 합니다."
                );
            }

            this.label = trimmedLabel;
        }

        this.minTarget = minTarget;
        this.maxTarget = maxTarget;
        this.rewardPoints = rewardPoints;
        this.rewardExp = rewardExp;
    }

    public int rollTarget(RandomGenerator random) {

        if (random == null) {
            throw new BusinessException(
                    "A random generator is required."
            );
        }

        return minTarget
                + random.nextInt(maxTarget - minTarget + 1);
    }

    /**
     * 운동 종류를 대문자로 통일합니다.
     */
    private String normalizeExerciseType(
            String exerciseType
    ) {

        if (exerciseType == null
                || exerciseType.isBlank()) {

            throw new BusinessException(
                    "운동 종류가 필요합니다."
            );
        }

        String normalized =
                exerciseType
                        .trim()
                        .toUpperCase(Locale.ROOT);

        if (normalized.length() > 30) {
            throw new BusinessException(
                    "운동 종류는 30자 이하여야 합니다."
            );
        }

        return normalized;
    }

    @PrePersist
    private void onCreate() {

        LocalDateTime now =
                LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void onUpdate() {

        this.updatedAt =
                LocalDateTime.now();
    }
}
