package com.smhrd.hometraining.crew.battle.entity;

import java.time.LocalDateTime;

import com.smhrd.hometraining.crew.entity.Crew;
import com.smhrd.hometraining.exercise.entity.ExerciseRecord;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "crew_battle_participants",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_battle_participant",
                        columnNames = {"battle_id", "user_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrewBattleParticipant {

    private static final int PERFECT_SCORE = 100;
    private static final int GREAT_SCORE = 80;
    private static final int GOOD_SCORE = 50;
    private static final int MISS_SCORE = 0;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 참여 중인 크루대전입니다.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "battle_id", nullable = false)
    private CrewBattle battle;

    /**
     * 대전에 참여한 사용자입니다.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 사용자가 크루대전에서 소속된 팀입니다.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crew_id", nullable = false)
    private Crew crew;

    /**
     * MISS를 제외하고 정상적으로 인정된 운동 횟수입니다.
     */
    @Column(name = "valid_count", nullable = false)
    private int validCount = 0;

    /**
     * 참가자가 획득한 누적 점수입니다.
     *
     * PERFECT: 100점
     * GREAT: 80점
     * GOOD: 50점
     * MISS: 0점
     */
    @Column(name = "total_score", nullable = false)
    private long totalScore = 0L;

    @Column(name = "perfect_count", nullable = false)
    private int perfectCount = 0;

    @Column(name = "great_count", nullable = false)
    private int greatCount = 0;

    @Column(name = "good_count", nullable = false)
    private int goodCount = 0;

    @Column(name = "miss_count", nullable = false)
    private int missCount = 0;

    /**
     * 이 대전의 개인 점수가 누적 기여도에 반영되었는지 표시합니다.
     */
    @Column(
            name = "contribution_recorded",
            nullable = false
    )
    private boolean contributionRecorded = false;

    /**
     * 가장 최근에 받은 자세 판정입니다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "latest_grade", length = 10)
    private ExerciseRecord.Grade latestGrade;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    /**
     * 여러 판정이 동시에 등록될 때 데이터 충돌을 방지합니다.
     */
    @Version
    private long version;

    @PrePersist
    void onCreate() {
        joinedAt = LocalDateTime.now();
    }

    /**
     * 새로운 크루대전 참가자를 생성합니다.
     */
    public static CrewBattleParticipant create(
            CrewBattle battle,
            User user,
            Crew crew
    ) {

        if (battle == null) {
            throw new IllegalArgumentException(
                    "크루대전 정보가 필요합니다."
            );
        }

        if (user == null) {
            throw new IllegalArgumentException(
                    "사용자 정보가 필요합니다."
            );
        }

        if (crew == null) {
            throw new IllegalArgumentException(
                    "크루 정보가 필요합니다."
            );
        }

        CrewBattleParticipant participant =
                new CrewBattleParticipant();

        participant.battle = battle;
        participant.user = user;
        participant.crew = crew;

        return participant;
    }

    /**
     * 운동 1회의 자세 판정 결과를 등록합니다.
     *
     * PERFECT, GREAT, GOOD은 인정 횟수에 포함되고
     * MISS는 전체 시도 횟수에만 포함됩니다.
     */
    public void registerRep(
            ExerciseRecord.Grade grade
    ) {

        if (grade == null) {
            throw new IllegalArgumentException(
                    "운동 판정 등급이 필요합니다."
            );
        }

        latestGrade = grade;

        switch (grade) {

            case PERFECT -> {
                perfectCount++;
                validCount++;
                addScore(PERFECT_SCORE);
            }

            case GREAT -> {
                greatCount++;
                validCount++;
                addScore(GREAT_SCORE);
            }

            case GOOD -> {
                goodCount++;
                validCount++;
                addScore(GOOD_SCORE);
            }

            case MISS -> {
                missCount++;
                addScore(MISS_SCORE);
            }
        }
    }

    /**
     * 점수를 안전하게 누적합니다.
     */
    private void addScore(
            int score
    ) {

        if (score <= 0) {
            return;
        }

        if (Long.MAX_VALUE - totalScore < score) {
            totalScore = Long.MAX_VALUE;
            return;
        }

        totalScore += score;
    }

    /**
     * MISS를 포함한 전체 운동 시도 횟수입니다.
     */
    public int getTotalAttempts() {
        return perfectCount
                + greatCount
                + goodCount
                + missCount;
    }

    public void markContributionRecorded() {
        this.contributionRecorded = true;
    }
}
