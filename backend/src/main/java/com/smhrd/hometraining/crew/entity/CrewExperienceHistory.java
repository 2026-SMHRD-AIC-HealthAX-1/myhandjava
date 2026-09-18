package com.smhrd.hometraining.crew.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "crew_experience_history",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_crew_exp_history_source",
                        columnNames = {
                                "crew_id",
                                "source_type",
                                "source_id"
                        }
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrewExperienceHistory {

    public enum SourceType {

        WEEKLY_MISSION,
        BATTLE_WIN,
        BATTLE_LOSS,
        BATTLE_DRAW,
        ADMIN_ADJUSTMENT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 경험치를 지급받은 크루입니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "crew_id",
            nullable = false
    )
    private Crew crew;

    /**
     * 경험치 지급 사유입니다.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "source_type",
            nullable = false,
            length = 30
    )
    private SourceType sourceType;

    /**
     * 관련 주간 미션 또는 크루대전 ID입니다.
     *
     * 같은 대상에 대한 경험치 중복 지급을
     * 방지하는 키로 사용합니다.
     */
    @Column(name = "source_id")
    private Long sourceId;

    /**
     * 실제 지급된 경험치입니다.
     *
     * 주간 한도 때문에 요청값보다
     * 적게 지급될 수 있습니다.
     */
    @Column(
            name = "exp_amount",
            nullable = false
    )
    private int expAmount;

    @Column(
            name = "level_before",
            nullable = false
    )
    private int levelBefore;

    @Column(
            name = "level_after",
            nullable = false
    )
    private int levelAfter;

    @Column(
            name = "exp_before",
            nullable = false
    )
    private int expBefore;

    @Column(
            name = "exp_after",
            nullable = false
    )
    private int expAfter;

    /**
     * 경험치가 적용된 주차의 월요일입니다.
     */
    @Column(
            name = "week_start",
            nullable = false
    )
    private LocalDate weekStart;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    /**
     * 크루 경험치 변경 이력을 생성합니다.
     */
    public static CrewExperienceHistory create(
            Crew crew,
            SourceType sourceType,
            Long sourceId,
            int expAmount,
            int levelBefore,
            int levelAfter,
            int expBefore,
            int expAfter,
            LocalDate weekStart
    ) {

        if (crew == null) {
            throw new IllegalArgumentException(
                    "크루 정보가 필요합니다."
            );
        }

        if (sourceType == null) {
            throw new IllegalArgumentException(
                    "경험치 지급 사유가 필요합니다."
            );
        }

        if (expAmount < 0) {
            throw new IllegalArgumentException(
                    "지급 경험치는 0 이상이어야 합니다."
            );
        }

        if (weekStart == null) {
            throw new IllegalArgumentException(
                    "경험치 적용 주차가 필요합니다."
            );
        }

        CrewExperienceHistory history =
                new CrewExperienceHistory();

        history.crew = crew;
        history.sourceType = sourceType;
        history.sourceId = sourceId;
        history.expAmount = expAmount;

        history.levelBefore = levelBefore;
        history.levelAfter = levelAfter;

        history.expBefore = expBefore;
        history.expAfter = expAfter;

        history.weekStart = weekStart;

        return history;
    }

    @PrePersist
    private void onCreate() {

        this.createdAt =
                LocalDateTime.now();
    }
}