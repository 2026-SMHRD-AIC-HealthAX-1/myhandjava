package com.smhrd.hometraining.mission.entity;

import java.time.LocalDate;

import com.smhrd.hometraining.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 사용자에게 실제로 배정된 개인 일일 미션입니다.
 */
@Entity
@Table(name = "missions")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Mission {

    public static final int DEFAULT_REWARD_POINTS = 50;
    public static final int DEFAULT_REWARD_EXP = 50;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 미션을 배정받은 사용자입니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    /**
     * 이 미션의 원본 관리자 미션입니다.
     *
     * 기존에 저장된 미션 데이터와의 호환성을 위해
     * null을 허용합니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "definition_id")
    private MissionDefinition definition;

    /**
     * 미션 진행 조건입니다.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MissionMetric metric;

    /**
     * 미션에 적용되는 운동 종류입니다.
     */
    @Column(
            name = "exercise_type",
            nullable = false,
            length = 30,
            columnDefinition = "varchar(30) default 'SQUAT'"
    )
    private String exerciseType = "SQUAT";

    /**
     * 미션 완료에 필요한 목표값입니다.
     */
    @Column(nullable = false)
    private int target;

    /**
     * 미션 완료 시 지급할 포인트입니다.
     *
     * 기존 코드와 DTO의 호환성을 위해
     * 필드 이름은 reward를 유지합니다.
     */
    @Column(
            nullable = false,
            columnDefinition = "int default 50"
    )
    private int reward = DEFAULT_REWARD_POINTS;

    /**
     * 미션 완료 시 지급할 경험치입니다.
     */
    @Column(
            name = "reward_exp",
            nullable = false,
            columnDefinition = "int default 50"
    )
    private int rewardExp = DEFAULT_REWARD_EXP;

    /**
     * 프론트엔드에 표시할 미션 이름입니다.
     */
    @Column(nullable = false, length = 100)
    private String label;

    /**
     * 미션이 배정된 대한민국 날짜입니다.
     */
    @Column(
            name = "assigned_date",
            nullable = false
    )
    private LocalDate assignedDate;

    /**
     * 사용자가 보상을 이미 수령했는지 나타냅니다.
     */
    @Column(nullable = false)
    private boolean claimed = false;

    /**
     * 관리자 미션 원본으로 사용자 일일 미션을 생성합니다.
     */
    public static Mission fromDefinition(
            User user,
            MissionDefinition definition,
            LocalDate assignedDate,
            int target
    ) {

        if (user == null) {
            throw new IllegalArgumentException(
                    "미션을 배정받을 사용자가 필요합니다."
            );
        }

        if (definition == null) {
            throw new IllegalArgumentException(
                    "관리자 미션 원본이 필요합니다."
            );
        }

        if (!definition.isActive()) {
            throw new IllegalArgumentException(
                    "비활성화된 미션은 배정할 수 없습니다."
            );
        }

        if (assignedDate == null) {
            throw new IllegalArgumentException(
                    "미션 배정 날짜가 필요합니다."
            );
        }

        if (definition.getScope() != MissionScope.PERSONAL) {
            throw new IllegalArgumentException(
                    "Only personal mission definitions can be assigned to users."
            );
        }

        if (target < definition.getMinTarget()
                || target > definition.getMaxTarget()) {
            throw new IllegalArgumentException(
                    "Assigned target is outside the configured range."
            );
        }

        Mission mission = new Mission();

        mission.user = user;
        mission.definition = definition;
        mission.metric = definition.getMetric();
        mission.exerciseType = definition.getExerciseType();
        mission.target = target;
        mission.reward = definition.getRewardPoints();
        mission.rewardExp = definition.getRewardExp();
        mission.label = definition.getLabel();
        mission.assignedDate = assignedDate;
        mission.claimed = false;

        return mission;
    }

    /**
     * 기존 랜덤 미션 생성 방식과의 호환성을 위해 유지합니다.
     *
     * 관리자 미션 원본이 없는 기존 처리에서만 사용합니다.
     */
    public static Mission generate(
            User user,
            MissionMetric metric,
            int target,
            LocalDate assignedDate
    ) {

        if (user == null) {
            throw new IllegalArgumentException(
                    "미션을 배정받을 사용자가 필요합니다."
            );
        }

        if (metric == null) {
            throw new IllegalArgumentException(
                    "미션 종류가 필요합니다."
            );
        }

        metric.validateTarget(target);

        if (assignedDate == null) {
            throw new IllegalArgumentException(
                    "미션 배정 날짜가 필요합니다."
            );
        }

        Mission mission = new Mission();

        mission.user = user;
        mission.definition = null;
        mission.metric = metric;
        mission.exerciseType = "SQUAT";
        mission.target = target;
        mission.reward = DEFAULT_REWARD_POINTS;
        mission.rewardExp = DEFAULT_REWARD_EXP;
        mission.label = metric.label(target);
        mission.assignedDate = assignedDate;
        mission.claimed = false;

        return mission;
    }

    /**
     * 미션 보상을 수령 완료 상태로 변경합니다.
     */
    public void markClaimed() {
        this.claimed = true;
    }
}
