package com.smhrd.hometraining.exercise.session;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smhrd.hometraining.common.exception.BusinessException;
import com.smhrd.hometraining.exercise.session.dto.DailyExerciseStatusResponse;
import com.smhrd.hometraining.exercise.session.dto.ExerciseSessionResponse;
import com.smhrd.hometraining.exercise.session.entity.ExerciseSession;
import com.smhrd.hometraining.exercise.session.repository.ExerciseSessionRepository;
import com.smhrd.hometraining.user.UserService;
import com.smhrd.hometraining.user.UserResourceHistoryService;
import com.smhrd.hometraining.user.entity.User;
import com.smhrd.hometraining.user.entity.UserResourceHistory.Reason;
import com.smhrd.hometraining.user.entity.UserResourceHistory.ResourceType;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExerciseSessionService {

    private final ExerciseSessionRepository exerciseSessionRepository;
    private final UserService userService;
    private final UserResourceHistoryService resourceHistoryService;
    private final EntityManager entityManager;

    /**
     * 오늘 무료 운동 이용 현황을 조회합니다.
     *
     * 날짜가 바뀌었다면 오늘 사용 횟수를
     * 자동으로 0으로 초기화합니다.
     */
    @Transactional
    public DailyExerciseStatusResponse getDailyExerciseStatus(
            Long userId
    ) {

        User user =
                userService.getUserOrThrow(userId);

        /*
         * 조회와 동시에 날짜 초기화가 발생할 수 있으므로
         * 사용자 행을 잠급니다.
         */
        entityManager.lock(
                user,
                LockModeType.PESSIMISTIC_WRITE
        );

        user.resetDailySetsIfNeeded();

        return DailyExerciseStatusResponse.from(user);
    }

    /**
     * 새로운 운동 세션을 생성합니다.
     *
     * 이 단계에서는 무료 횟수나 티켓을
     * 차감하지 않습니다.
     */
    @Transactional
    public ExerciseSessionResponse createSession(
            Long userId,
            String exerciseType
    ) {

        User user =
                userService.getUserOrThrow(userId);

        user.resetDailySetsIfNeeded();

        boolean hasFreeUsage =
                hasFreeUsage(user);

        boolean hasRetakeTicket =
                user.getRetakeTickets() > 0;

        if (!hasFreeUsage && !hasRetakeTicket) {
            throw new BusinessException(
                    "오늘 가능한 무료 운동 횟수를 모두 사용했고 다시찍기 티켓도 없습니다."
            );
        }

        ExerciseSession session =
                ExerciseSession.create(
                        user,
                        exerciseType
                );

        /*
         * 세션 생성 시점의 이용 가능 상태를 표시합니다.
         *
         * 실제 차감은 startSession에서
         * 최신 상태를 다시 확인한 후 처리합니다.
         */
        if (hasFreeUsage) {
            session.configureFreeExercise();
        } else {
            session.configureRetakeTicket();
        }

        ExerciseSession savedSession =
                exerciseSessionRepository.save(session);

        return ExerciseSessionResponse.from(
                savedSession
        );
    }

    /**
     * 카메라와 자세 인식이 정상적으로 준비된 후
     * 실제 운동 시작을 처리합니다.
     */
    @Transactional
    public ExerciseSessionResponse startSession(
            Long userId,
            String sessionId
    ) {

        ExerciseSession session =
                getSessionForUpdate(
                        userId,
                        sessionId
                );

        /*
         * 가장 먼저 세션 상태를 검사합니다.
         *
         * 이미 STARTED, COMPLETED, ABORTED, FAILED 상태라면
         * 무료 횟수나 티켓을 건드리기 전에 요청을 차단합니다.
         */
        if (!session.getStatus().canStart()) {
            throw new BusinessException(
                    "이미 시작됐거나 종료된 운동 세션입니다."
            );
        }

        User user = session.getUser();

        /*
         * 한 사용자의 서로 다른 세션에서 시작 요청이
         * 동시에 들어오는 상황을 막기 위해
         * 사용자 DB 행도 잠급니다.
         */
        entityManager.lock(
                user,
                LockModeType.PESSIMISTIC_WRITE
        );

        user.resetDailySetsIfNeeded();

        /*
         * 실제 시작 시점의 최신 무료 횟수와
         * 티켓 보유량을 다시 확인합니다.
         */
        if (hasFreeUsage(user)) {

            user.setSetsUsedToday(
                    user.getSetsUsedToday() + 1
            );

            session.configureFreeExercise();

        } else if (user.getRetakeTickets() > 0) {

            int ticketsBefore = user.getRetakeTickets();

            user.setRetakeTickets(
                    ticketsBefore - 1
            );

            resourceHistoryService.record(
                    user,
                    ResourceType.RETAKE_TICKET,
                    -1,
                    ticketsBefore,
                    user.getRetakeTickets(),
                    Reason.EXERCISE_SESSION,
                    session.getSessionId()
            );

            session.configureRetakeTicket();

        } else {

            throw new BusinessException(
                    "사용 가능한 무료 운동 횟수나 다시찍기 티켓이 없습니다."
            );
        }

        /*
         * 무료 횟수 또는 티켓 차감과
         * STARTED 상태 변경은 같은 트랜잭션에서 처리됩니다.
         */
        session.markStarted();

        return ExerciseSessionResponse.from(session);
    }

    /**
     * 카메라 권한 거부, 연결 실패 또는
     * 자세 인식 초기화 실패를 처리합니다.
     *
     * 운동 시작 전 실패이므로
     * 사용 횟수나 티켓을 차감하지 않습니다.
     */
    @Transactional
    public ExerciseSessionResponse failSession(
            Long userId,
            String sessionId,
            String reason
    ) {

        ExerciseSession session =
                getSessionForUpdate(
                        userId,
                        sessionId
                );

        session.markFailed(reason);

        return ExerciseSessionResponse.from(session);
    }

    /**
     * 운동 시작 후 사용자가 중간에 나간 경우입니다.
     *
     * 이미 사용된 무료 횟수나 티켓은
     * 복구하지 않습니다.
     */
    @Transactional
    public ExerciseSessionResponse abortSession(
            Long userId,
            String sessionId
    ) {

        ExerciseSession session =
                getSessionForUpdate(
                        userId,
                        sessionId
                );

        session.markAborted();

        return ExerciseSessionResponse.from(session);
    }

    /**
     * 본인의 운동 세션 한 건을 조회합니다.
     */
    @Transactional(readOnly = true)
    public ExerciseSessionResponse getSession(
            Long userId,
            String sessionId
    ) {

        ExerciseSession session =
                exerciseSessionRepository
                        .findBySessionIdAndUserId(
                                sessionId,
                                userId
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        "운동 세션을 찾을 수 없습니다."
                                )
                        );

        return ExerciseSessionResponse.from(session);
    }

    /**
     * 사용자의 운동 세션 내역을 최신순으로 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<ExerciseSessionResponse> getSessionHistory(
            Long userId
    ) {

        return exerciseSessionRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(ExerciseSessionResponse::from)
                .toList();
    }

    /**
     * 운동 결과 저장에서 사용할 세션 잠금 조회입니다.
     */
    @Transactional
    public ExerciseSession getSessionEntityForUpdate(
            Long userId,
            String sessionId
    ) {

        return getSessionForUpdate(
                userId,
                sessionId
        );
    }

    /**
     * 오늘 사용할 수 있는 무료 운동 횟수가
     * 남아 있는지 확인합니다.
     */
    private boolean hasFreeUsage(User user) {

        return user.getSetsUsedToday()
                < user.getDailySetLimit();
    }

    /**
     * 세션 상태 변경을 위한 DB 잠금 조회입니다.
     */
    private ExerciseSession getSessionForUpdate(
            Long userId,
            String sessionId
    ) {

        if (sessionId == null || sessionId.isBlank()) {
            throw new BusinessException(
                    "운동 세션 ID가 필요합니다."
            );
        }

        return exerciseSessionRepository
                .findBySessionIdAndUserIdForUpdate(
                        sessionId,
                        userId
                )
                .orElseThrow(() ->
                        new BusinessException(
                                "운동 세션을 찾을 수 없습니다."
                        )
                );
    }
}
