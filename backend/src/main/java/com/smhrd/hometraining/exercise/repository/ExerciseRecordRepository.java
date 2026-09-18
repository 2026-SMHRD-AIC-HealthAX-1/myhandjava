package com.smhrd.hometraining.exercise.repository;

import com.smhrd.hometraining.exercise.entity.ExerciseRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ExerciseRecordRepository extends JpaRepository<ExerciseRecord, Long> {
    List<ExerciseRecord> findByUserIdOrderByRecordedAtDesc(Long userId);

    @Query("select coalesce(sum(r.pointsAwarded), 0) from ExerciseRecord r where r.user.id = :userId")
    long sumPointsAwardedByUserId(Long userId);

    @Query("select coalesce(sum(r.score), 0) from ExerciseRecord r where r.user.id in :userIds")
    long sumScoreByUserIds(@Param("userIds") Collection<Long> userIds);

    @Query("select coalesce(sum(r.reps), 0) from ExerciseRecord r " +
            "where r.user.id in :userIds and r.exerciseType = :exerciseType " +
            "and r.recordedAt >= :from and r.recordedAt < :to")
    long sumRepsByUserIdsAndExerciseTypeAndPeriod(@Param("userIds") Collection<Long> userIds,
                                                   @Param("exerciseType") String exerciseType,
                                                   @Param("from") LocalDateTime from,
                                                   @Param("to") LocalDateTime to);
}
