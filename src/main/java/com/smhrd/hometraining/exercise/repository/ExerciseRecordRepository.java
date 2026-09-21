package com.smhrd.hometraining.exercise.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smhrd.hometraining.exercise.entity.ExerciseRecord;

public interface ExerciseRecordRepository
        extends JpaRepository<ExerciseRecord, Long> {

    /**
     * 사용자의 운동 기록을 최신순으로 조회합니다.
     */
    List<ExerciseRecord> findByUserIdOrderByRecordedAtDesc(
            Long userId
    );

    /**
     * 특정 세션의 운동 결과가 이미 저장됐는지 확인합니다.
     *
     * 같은 결과를 두 번 전송하는 것을
     * 서비스에서 미리 차단할 때 사용합니다.
     */
    boolean existsBySessionSessionId(
            String sessionId
    );

    /**
     * 세션 ID로 저장된 운동 기록을 조회합니다.
     */
    Optional<ExerciseRecord> findBySessionSessionId(
            String sessionId
    );

    /**
     * 특정 사용자의 특정 세션 결과를 조회합니다.
     */
    Optional<ExerciseRecord> findBySessionSessionIdAndUserId(
            String sessionId,
            Long userId
    );

    Optional<ExerciseRecord> findByUserIdAndIdempotencyKey(
            Long userId,
            String idempotencyKey
    );

    /**
     * 사용자가 운동으로 획득한 포인트 총합을 조회합니다.
     */
    @Query("""
            SELECT COALESCE(SUM(record.pointsAwarded), 0)
            FROM ExerciseRecord record
            WHERE record.user.id = :userId
            """)
    long sumPointsAwardedByUserId(
            @Param("userId")
            Long userId
    );

    /**
     * 여러 사용자의 운동 점수 총합을 조회합니다.
     */
    @Query("""
            SELECT COALESCE(SUM(record.score), 0)
            FROM ExerciseRecord record
            WHERE record.user.id IN :userIds
            """)
    long sumScoreByUserIds(
            @Param("userIds")
            Collection<Long> userIds
    );

    /**
     * 지정한 기간 동안 여러 사용자가 수행한
     * 특정 운동의 횟수 합계를 조회합니다.
     */
    @Query("""
            SELECT COALESCE(SUM(record.reps), 0)
            FROM ExerciseRecord record
            WHERE record.user.id IN :userIds
              AND record.exerciseType = :exerciseType
              AND record.recordedAt >= :from
              AND record.recordedAt < :to
            """)
    long sumRepsByUserIdsAndExerciseTypeAndPeriod(
            @Param("userIds")
            Collection<Long> userIds,

            @Param("exerciseType")
            String exerciseType,

            @Param("from")
            LocalDateTime from,

            @Param("to")
            LocalDateTime to
    );
    
    /**
     * 특정 사용자가 지정한 기간 동안 수행한
     * 특정 운동의 전체 횟수를 조회합니다.
     *
     * includeRetakeTicket이 true이면
     * 다시찍기 티켓 운동도 포함합니다.
     *
     * false이면 무료 운동 기록만 포함합니다.
     */
    @Query("""
            SELECT COALESCE(SUM(record.reps), 0)
            FROM ExerciseRecord record
            WHERE record.user.id = :userId
              AND record.exerciseType = :exerciseType
              AND record.recordedAt >= :from
              AND record.recordedAt < :to
              AND (
                    :includeRetakeTicket = true
                    OR record.session.rewardEligible = true
              )
            """)
    long sumRepsByUserIdAndExerciseTypeAndPeriod(
            @Param("userId")
            Long userId,

            @Param("exerciseType")
            String exerciseType,

            @Param("from")
            LocalDateTime from,

            @Param("to")
            LocalDateTime to,

            @Param("includeRetakeTicket")
            boolean includeRetakeTicket
    );

    void deleteByUserId(Long userId);

}
