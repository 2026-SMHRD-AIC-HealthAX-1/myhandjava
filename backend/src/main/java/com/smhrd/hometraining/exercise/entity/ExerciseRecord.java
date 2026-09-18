package com.smhrd.hometraining.exercise.entity;

import com.smhrd.hometraining.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "exercise_records")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExerciseRecord {

    public enum Grade { PERFECT, GREAT, GOOD, MISS }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "exercise_type", nullable = false, length = 20)
    private String exerciseType;

    @Column(nullable = false)
    private int reps;

    @Column(nullable = false)
    private int accuracy;

    @Column(nullable = false)
    private int score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Grade grade;

    @Column(name = "perfect_count", nullable = false)
    private int perfectCount;

    @Column(name = "great_count", nullable = false)
    private int greatCount;

    @Column(name = "good_count", nullable = false)
    private int goodCount;

    @Column(name = "miss_count", nullable = false)
    private int missCount;

    @Column(name = "points_awarded", nullable = false)
    private int pointsAwarded;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    public static Grade gradeFromAccuracy(int accuracy) {
        if (accuracy > 90) return Grade.PERFECT;
        if (accuracy > 78) return Grade.GREAT;
        if (accuracy > 60) return Grade.GOOD;
        return Grade.MISS;
    }

    public static ExerciseRecord create(User user, String exerciseType, int reps, int accuracy, int score,
                                         int perfectCount, int greatCount, int goodCount, int missCount,
                                         int pointsAwarded) {
        ExerciseRecord record = new ExerciseRecord();
        record.user = user;
        record.exerciseType = exerciseType;
        record.reps = reps;
        record.accuracy = accuracy;
        record.score = score;
        record.grade = gradeFromAccuracy(accuracy);
        record.perfectCount = perfectCount;
        record.greatCount = greatCount;
        record.goodCount = goodCount;
        record.missCount = missCount;
        record.pointsAwarded = pointsAwarded;
        record.recordedAt = LocalDateTime.now();
        return record;
    }
}
