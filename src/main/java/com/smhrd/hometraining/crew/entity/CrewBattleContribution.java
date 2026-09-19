package com.smhrd.hometraining.crew.entity;

import java.time.LocalDateTime;

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
        name = "crew_battle_contributions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_crew_battle_contribution",
                        columnNames = {"crew_id", "user_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrewBattleContribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crew_id", nullable = false)
    private Crew crew;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "total_score", nullable = false)
    private long totalScore = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static CrewBattleContribution create(
            Crew crew,
            User user
    ) {

        if (crew == null || user == null) {
            throw new IllegalArgumentException(
                    "크루와 사용자 정보가 필요합니다."
            );
        }

        CrewBattleContribution contribution =
                new CrewBattleContribution();

        contribution.crew = crew;
        contribution.user = user;

        return contribution;
    }

    public void addScore(long score) {

        long safeScore = Math.max(score, 0L);

        if (Long.MAX_VALUE - totalScore < safeScore) {
            totalScore = Long.MAX_VALUE;
            return;
        }

        totalScore += safeScore;
    }

    @PrePersist
    private void onCreate() {

        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    private void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
