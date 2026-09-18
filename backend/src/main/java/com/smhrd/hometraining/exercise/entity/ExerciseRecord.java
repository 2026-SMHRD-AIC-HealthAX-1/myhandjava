package com.smhrd.hometraining.exercise.entity;

import java.time.LocalDateTime;

import com.smhrd.hometraining.exercise.session.entity.ExerciseSession;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "exercise_records",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_exercise_records_session_id",
                        columnNames = "session_id"
                ),
                @UniqueConstraint(
                        name = "uk_exercise_records_user_idempotency",
                        columnNames = {
                                "user_id",
                                "idempotency_key"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExerciseRecord {

    public enum Grade {
        PERFECT,
        GREAT,
        GOOD,
        MISS
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    /**
     * 이 기록을 생성한 운동 세션입니다.
     *
     * 기존 운동 기록에는 세션 정보가 없기 때문에
     * NULL을 허용합니다.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "session_id",
            unique = true
    )
    private ExerciseSession session;

    @Column(
            name = "idempotency_key",
            length = 64,
            updatable = false
    )
    private String idempotencyKey;

    @Column(
            name = "exercise_type",
            nullable = false,
            length = 20
    )
    private String exerciseType;

    @Column(nullable = false)
    private int reps;

    @Column(nullable = false)
    private int accuracy;

    @Column(nullable = false)
    private int score;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 10
    )
    private Grade grade;

    @Column(
            name = "perfect_count",
            nullable = false
    )
    private int perfectCount;

    @Column(
            name = "great_count",
            nullable = false
    )
    private int greatCount;

    @Column(
            name = "good_count",
            nullable = false
    )
    private int goodCount;

    @Column(
            name = "miss_count",
            nullable = false
    )
    private int missCount;

    /**
     * 이 운동 기록으로 실제 지급된 경험치입니다.
     *
     * 다시찍기 티켓 운동은 0으로 저장됩니다.
     */
    @Column(
            name = "exp_awarded",
            nullable = false
    )
    private int expAwarded;

    /**
     * 이 운동 기록으로 실제 지급된 포인트입니다.
     *
     * 다시찍기 티켓 운동은 0으로 저장됩니다.
     */
    @Column(
            name = "points_awarded",
            nullable = false
    )
    private int pointsAwarded;

    @Column(
            name = "recorded_at",
            nullable = false
    )
    private LocalDateTime recordedAt;

    /**
     * 운동 정확도에 따라 대표 등급을 결정합니다.
     */
    public static Grade gradeFromAccuracy(int accuracy) {

        if (accuracy > 90) {
            return Grade.PERFECT;
        }

        if (accuracy > 78) {
            return Grade.GREAT;
        }

        if (accuracy > 60) {
            return Grade.GOOD;
        }

        return Grade.MISS;
    }

    /**
     * 세션과 연결된 새로운 운동 기록을 생성합니다.
     *
     * 지급 경험치와 포인트를 모두 전달받습니다.
     */
    public static ExerciseRecord create(
            User user,
            ExerciseSession session,
            String idempotencyKey,
            String exerciseType,
            int reps,
            int accuracy,
            int score,
            int perfectCount,
            int greatCount,
            int goodCount,
            int missCount,
            int expAwarded,
            int pointsAwarded
    ) {

        ExerciseRecord record =
                new ExerciseRecord();

        record.user = user;
        record.session = session;
        record.idempotencyKey = idempotencyKey;
        record.exerciseType = exerciseType;
        record.reps = reps;
        record.accuracy = accuracy;
        record.score = score;

        record.grade =
                gradeFromAccuracy(accuracy);

        record.perfectCount = perfectCount;
        record.greatCount = greatCount;
        record.goodCount = goodCount;
        record.missCount = missCount;

        record.expAwarded = expAwarded;
        record.pointsAwarded = pointsAwarded;

        record.recordedAt = LocalDateTime.now();

        return record;
    }

    /**
     * 기존 서비스 코드의 컴파일 오류를 방지하기 위한
     * 임시 호환 메서드입니다.
     *
     * 다음 단계에서 서비스 코드를 변경한 후에는
     * 이 메서드를 사용하지 않습니다.
     */
    public static ExerciseRecord create(
            User user,
            ExerciseSession session,
            String exerciseType,
            int reps,
            int accuracy,
            int score,
            int perfectCount,
            int greatCount,
            int goodCount,
            int missCount,
            int pointsAwarded
    ) {

        return create(
                user,
                session,
                session == null
                        ? null
                        : session.getSessionId(),
                exerciseType,
                reps,
                accuracy,
                score,
                perfectCount,
                greatCount,
                goodCount,
                missCount,
                0,
                pointsAwarded
        );
    }

    /**
     * 기존 세션 기능 적용 전 코드와 테스트를 위한
     * 호환 메서드입니다.
     */
    public static ExerciseRecord create(
            User user,
            String exerciseType,
            int reps,
            int accuracy,
            int score,
            int perfectCount,
            int greatCount,
            int goodCount,
            int missCount,
            int pointsAwarded
    ) {

        return create(
                user,
                null,
                null,
                exerciseType,
                reps,
                accuracy,
                score,
                perfectCount,
                greatCount,
                goodCount,
                missCount,
                0,
                pointsAwarded
        );
    }
}
