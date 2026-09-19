package com.smhrd.hometraining.exercise.session.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.smhrd.hometraining.common.exception.BusinessException;
import com.smhrd.hometraining.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
        name = "exercise_sessions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_exercise_sessions_session_id",
                        columnNames = "session_id"
                )
        },
        indexes = {
                @Index(
                        name = "idx_exercise_sessions_user_created",
                        columnList = "user_id, created_at"
                ),
                @Index(
                        name = "idx_exercise_sessions_status",
                        columnList = "status"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExerciseSession {

    /**
     * 한 세트의 최대 운동 시간입니다.
     */
    public static final long MAX_DURATION_SECONDS = 120L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 프론트와 서버가 운동 한 세트를 구분할 때 사용하는 값입니다.
     *
     * DB 기본키를 외부에 노출하지 않고 UUID를 사용합니다.
     */
    @Column(
            name = "session_id",
            nullable = false,
            length = 36,
            updatable = false
    )
    private String sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    @Column(
            name = "exercise_type",
            nullable = false,
            length = 20
    )
    private String exerciseType;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private ExerciseSessionStatus status;

    /**
     * 실제 운동이 시작되어 무료 횟수나 티켓이
     * 차감됐는지 표시합니다.
     */
    @Column(
            name = "usage_charged",
            nullable = false
    )
    private boolean usageCharged;

    /**
     * 이 세션의 운동 결과가 이미 저장됐는지 표시합니다.
     *
     * 같은 결과가 두 번 저장되는 것을 차단할 때 사용합니다.
     */
    @Column(
            name = "result_saved",
            nullable = false
    )
    private boolean resultSaved;

    /**
     * 경험치와 포인트를 지급할 수 있는 세션인지 표시합니다.
     *
     * 무료 운동은 true,
     * 다시찍기 티켓 운동은 false로 사용할 예정입니다.
     */
    @Column(
            name = "reward_eligible",
            nullable = false
    )
    private boolean rewardEligible;

    /**
     * 다시찍기 티켓을 사용하는 세션인지 표시합니다.
     */
    @Column(
            name = "ticket_used",
            nullable = false
    )
    private boolean ticketUsed;

    @Column(
            name = "failure_reason",
            length = 200
    )
    private String failureReason;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    /**
     * 동일한 세션에 동시에 여러 요청이 들어오는 경우
     * 중복 변경을 막기 위한 버전 값입니다.
     */
    @Version
    @Column(
            name = "version",
            nullable = false
    )
    private Long version;

    /**
     * 새로운 운동 세션을 생성합니다.
     *
     * 이 시점에는 아직 실제 운동이 시작되지 않았으므로
     * 운동 횟수나 티켓을 차감하지 않습니다.
     */
    public static ExerciseSession create(
            User user,
            String exerciseType
    ) {

        if (user == null) {
            throw new BusinessException(
                    "운동 세션 사용자 정보가 필요합니다."
            );
        }

        if (exerciseType == null
                || exerciseType.isBlank()) {

            throw new BusinessException(
                    "운동 종류가 필요합니다."
            );
        }

        ExerciseSession session =
                new ExerciseSession();

        session.sessionId =
                UUID.randomUUID().toString();

        session.user = user;
        session.exerciseType = exerciseType.trim();
        session.status = ExerciseSessionStatus.CREATED;

        session.usageCharged = false;
        session.resultSaved = false;

        /*
         * 무료 운동 여부와 티켓 사용 여부는
         * 이후 세션 서비스에서 최종 결정합니다.
         */
        session.rewardEligible = true;
        session.ticketUsed = false;

        session.createdAt = LocalDateTime.now();

        return session;
    }

    /**
     * 카메라와 자세 인식이 정상적으로 준비된 후
     * 운동 시작 상태로 변경합니다.
     */
    public void markStarted() {

        if (!status.canStart()) {
            throw new BusinessException(
                    "시작할 수 없는 운동 세션 상태입니다."
            );
        }

        status = ExerciseSessionStatus.STARTED;
        startedAt = LocalDateTime.now();
        usageCharged = true;
        failureReason = null;
    }

    /**
     * 운동 결과 저장이 끝난 후
     * 완료 상태로 변경합니다.
     */
    public void markCompleted() {

        if (!status.canComplete()) {
            throw new BusinessException(
                    "완료할 수 없는 운동 세션 상태입니다."
            );
        }

        if (resultSaved) {
            throw new BusinessException(
                    "이미 결과가 저장된 운동 세션입니다."
            );
        }

        status = ExerciseSessionStatus.COMPLETED;
        resultSaved = true;
        endedAt = LocalDateTime.now();
    }

    /**
     * 정상 시작 후 사용자가 중간에 나간 경우입니다.
     *
     * 이미 사용 횟수가 차감된 상태이므로
     * usageCharged는 true로 유지합니다.
     */
    public void markAborted() {

        if (status != ExerciseSessionStatus.STARTED) {
            throw new BusinessException(
                    "운동 중인 세션만 중도 종료할 수 있습니다."
            );
        }

        status = ExerciseSessionStatus.ABORTED;
        endedAt = LocalDateTime.now();
    }

    /**
     * 카메라 권한 거부 또는 연결 실패처럼
     * 운동 시작 전에 발생한 실패를 처리합니다.
     */
    public void markFailed(String reason) {

        if (status != ExerciseSessionStatus.CREATED) {
            throw new BusinessException(
                    "운동 시작 전 세션만 실패 처리할 수 있습니다."
            );
        }

        status = ExerciseSessionStatus.FAILED;
        usageCharged = false;
        failureReason = normalizeFailureReason(reason);
        endedAt = LocalDateTime.now();
    }

    /**
     * 무료 운동 세션으로 설정합니다.
     */
    public void configureFreeExercise() {
        rewardEligible = true;
        ticketUsed = false;
    }

    /**
     * 다시찍기 티켓 운동 세션으로 설정합니다.
     *
     * 티켓 운동은 기록과 최고점에는 반영할 수 있지만,
     * 경험치와 포인트는 지급하지 않습니다.
     */
    public void configureRetakeTicket() {
        rewardEligible = false;
        ticketUsed = true;
    }

    /**
     * 이 세션이 해당 사용자의 세션인지 확인합니다.
     */
    public boolean belongsTo(Long userId) {

        return userId != null
                && user != null
                && user.getId() != null
                && user.getId().equals(userId);
    }

    /**
     * 운동 시작 후 제한시간 120초가 지났는지 확인합니다.
     */
    public boolean hasExceededTimeLimit(
            LocalDateTime currentTime
    ) {

        if (startedAt == null || currentTime == null) {
            return false;
        }

        return !currentTime.isBefore(
                startedAt.plusSeconds(
                        MAX_DURATION_SECONDS
                )
        );
    }

    /**
     * DB 저장 직전 누락된 기본값을 보완합니다.
     */
    @PrePersist
    void onCreate() {

        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }

        if (status == null) {
            status = ExerciseSessionStatus.CREATED;
        }

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /**
     * 실패 사유를 DB 컬럼 길이에 맞게 정리합니다.
     */
    private String normalizeFailureReason(String reason) {

        if (reason == null || reason.isBlank()) {
            return "알 수 없는 시작 실패";
        }

        String trimmedReason = reason.trim();

        if (trimmedReason.length() <= 200) {
            return trimmedReason;
        }

        return trimmedReason.substring(0, 200);
    }
}