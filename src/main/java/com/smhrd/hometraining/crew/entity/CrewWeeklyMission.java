package com.smhrd.hometraining.crew.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.smhrd.hometraining.crew.policy.CrewWeeklyMissionPolicy;
import com.smhrd.hometraining.mission.entity.MissionDefinition;
import com.smhrd.hometraining.mission.entity.MissionScope;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "crew_weekly_missions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_crew_weekly_mission",
                        columnNames = {
                                "crew_id",
                                "week_start"
                        }
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrewWeeklyMission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 주간 미션을 수행하는 크루입니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "crew_id",
            nullable = false
    )
    private Crew crew;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "definition_id",
            nullable = false
    )
    private MissionDefinition definition;

    @Column(
            name = "exercise_type",
            nullable = false,
            length = 30
    )
    private String exerciseType;

    @Column(nullable = false, length = 100)
    private String label;

    /**
     * 해당 주의 시작 날짜인 월요일입니다.
     */
    @Column(
            name = "week_start",
            nullable = false
    )
    private LocalDate weekStart;

    /**
     * 해당 주의 종료 날짜인 일요일입니다.
     */
    @Column(
            name = "week_end",
            nullable = false
    )
    private LocalDate weekEnd;

    /**
     * 크루 전체 주간 목표 횟수입니다.
     */
    @Column(
            name = "target_reps",
            nullable = false
    )
    private int targetReps =
            CrewWeeklyMissionPolicy.WEEKLY_TARGET_REPS;

    /**
     * 현재 인정된 크루 전체 운동 횟수입니다.
     */
    @Column(
            name = "current_reps",
            nullable = false
    )
    private int currentReps = 0;

    /**
     * 주간 미션 완료 시 지급할 크루 경험치입니다.
     */
    @Column(
            name = "reward_exp",
            nullable = false
    )
    private int rewardExp =
            CrewWeeklyMissionPolicy.COMPLETION_REWARD_EXP;

    /**
     * 주간 목표 달성 여부입니다.
     */
    @Column(nullable = false)
    private boolean completed = false;

    /**
     * 이번 주 보상 지급 여부입니다.
     *
     * true이면 같은 주에 다시 보상하지 않습니다.
     */
    @Column(
            name = "reward_granted",
            nullable = false
    )
    private boolean rewardGranted = false;

    /**
     * 목표를 달성한 시간입니다.
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    /**
     * 새로운 주간 미션을 생성합니다.
     */
    public static CrewWeeklyMission startFor(
            Crew crew,
            MissionDefinition definition,
            int targetReps,
            LocalDate weekStart
    ) {

        if (crew == null) {
            throw new IllegalArgumentException(
                    "크루 정보가 필요합니다."
            );
        }

        if (weekStart == null) {
            throw new IllegalArgumentException(
                    "주간 시작 날짜가 필요합니다."
            );
        }

        if (definition == null
                || definition.getScope() != MissionScope.CREW
                || !definition.isActive()) {
            throw new IllegalArgumentException(
                    "An active crew mission definition is required."
            );
        }

        if (targetReps < definition.getMinTarget()
                || targetReps > definition.getMaxTarget()) {
            throw new IllegalArgumentException(
                    "Crew mission target is outside the configured range."
            );
        }

        CrewWeeklyMission mission =
                new CrewWeeklyMission();

        mission.crew = crew;
        mission.definition = definition;
        mission.exerciseType = definition.getExerciseType();
        mission.label = definition.getLabel();
        mission.weekStart = weekStart;
        mission.weekEnd =
                CrewWeeklyMissionPolicy.getWeekEnd(
                        weekStart
                );

        mission.targetReps = targetReps;

        mission.currentReps = 0;

        mission.rewardExp = definition.getRewardExp();

        mission.completed = false;
        mission.rewardGranted = false;

        return mission;
    }

    /**
     * 크루원별 인정 횟수를 다시 합산한 값으로
     * 현재 진행도를 갱신합니다.
     */
    public void updateProgress(
            int calculatedReps
    ) {

        this.currentReps =
                Math.min(
                        Math.max(calculatedReps, 0),
                        targetReps
                );

        if (!completed
                && currentReps >= targetReps) {

            this.completed = true;
            this.completedAt =
                    LocalDateTime.now();
        }
    }

    /**
     * 주간 미션 보상을 지급 완료 상태로 변경합니다.
     */
    public void markRewardGranted() {

        if (!completed) {
            throw new IllegalStateException(
                    "완료되지 않은 주간 미션입니다."
            );
        }

        if (rewardGranted) {
            throw new IllegalStateException(
                    "이미 보상이 지급된 주간 미션입니다."
            );
        }

        this.rewardGranted = true;
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
