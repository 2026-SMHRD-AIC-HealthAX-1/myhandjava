package com.smhrd.hometraining.exercise.session.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smhrd.hometraining.exercise.session.entity.ExerciseSession;

import jakarta.persistence.LockModeType;

public interface ExerciseSessionRepository
        extends JpaRepository<ExerciseSession, Long> {

    /**
     * 세션 ID로 운동 세션을 조회합니다.
     */
    Optional<ExerciseSession> findBySessionId(
            String sessionId
    );

    /**
     * 세션 ID와 사용자 ID를 함께 검사하여 조회합니다.
     *
     * 다른 사용자의 세션에 접근하는 것을 차단할 때 사용합니다.
     */
    Optional<ExerciseSession> findBySessionIdAndUserId(
            String sessionId,
            Long userId
    );

    /**
     * 사용자의 운동 세션을 최신순으로 조회합니다.
     */
    List<ExerciseSession> findByUserIdOrderByCreatedAtDesc(
            Long userId
    );

    /**
     * 동일한 sessionId가 이미 존재하는지 확인합니다.
     */
    boolean existsBySessionId(
            String sessionId
    );

    /**
     * 세션 상태를 변경할 때 사용하는 잠금 조회입니다.
     *
     * 동일한 운동 완료 요청이 동시에 두 번 도착해도
     * 한 요청이 처리되는 동안 다른 요청이 같은 세션을
     * 변경하지 못하도록 DB 행을 잠급니다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT session
            FROM ExerciseSession session
            WHERE session.sessionId = :sessionId
              AND session.user.id = :userId
            """)
    Optional<ExerciseSession> findBySessionIdAndUserIdForUpdate(
            @Param("sessionId") String sessionId,
            @Param("userId") Long userId
    );

    void deleteByUserId(Long userId);
}