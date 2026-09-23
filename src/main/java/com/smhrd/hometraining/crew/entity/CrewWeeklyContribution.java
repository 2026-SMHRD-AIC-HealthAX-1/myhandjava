package com.smhrd.hometraining.crew.entity;

import java.time.LocalDateTime;

import com.smhrd.hometraining.crew.policy.CrewWeeklyMissionPolicy;
import com.smhrd.hometraining.user.entity.User;

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
        name = "crew_weekly_contributions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_crew_weekly_contribution",
                        columnNames = {
                                "weekly_mission_id",
                                "user_id"
                        }
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrewWeeklyContribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 해당 크루의 주간 미션입니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "weekly_mission_id",
            nullable = false
    )
    private CrewWeeklyMission weeklyMission;

    /**
     * 운동에 기여한 크루원입니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    /**
     * 해당 주에 사용자가 실제로 수행한
     * 전체 운동 횟수입니다.
     */
    @Column(
            name = "total_reps",
            nullable = false
    )
    private int totalReps = 0;

    /**
     * 최대 기준을 적용한
     * 크루 미션 인정 횟수입니다.
     */
    @Column(
            name = "recognized_reps",
            nullable = false
    )
    private int recognizedReps = 0;

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
     * 크루원의 주간 기여도 정보를 생성합니다.
     */
    public static CrewWeeklyContribution startFor(
            CrewWeeklyMission weeklyMission,
            User user
    ) {

        if (weeklyMission == null) {
            throw new IllegalArgumentException(
                    "크루 주간 미션 정보가 필요합니다."
            );
        }

        if (user == null) {
            throw new IllegalArgumentException(
                    "크루원 정보가 필요합니다."
            );
        }

        CrewWeeklyContribution contribution =
                new CrewWeeklyContribution();

        contribution.weeklyMission =
                weeklyMission;

        contribution.user =
                user;

        contribution.totalReps = 0;
        contribution.recognizedReps = 0;

        return contribution;
    }

    /**
     * 사용자의 실제 주간 운동 횟수(GOOD 이상)를 저장하고
     * 한 명당 최대 80회까지만 인정합니다.
     */
    public void updateTotalReps(
            int totalReps
    ) {

        this.totalReps =
                Math.max(totalReps, 0);

        this.recognizedReps =
                CrewWeeklyMissionPolicy
                        .capMemberReps(
                                this.totalReps
                        );
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