package com.smhrd.hometraining.exercise.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.smhrd.hometraining.common.exception.BusinessException;
import com.smhrd.hometraining.exercise.session.dto.ExerciseSessionResponse;
import com.smhrd.hometraining.exercise.session.entity.ExerciseSession;
import com.smhrd.hometraining.exercise.session.entity.ExerciseSessionStatus;
import com.smhrd.hometraining.exercise.session.repository.ExerciseSessionRepository;
import com.smhrd.hometraining.user.UserService;
import com.smhrd.hometraining.user.entity.User;
import com.smhrd.hometraining.user.entity.UserGrade;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

class ExerciseSessionServiceTest {

    private ExerciseSessionRepository exerciseSessionRepository;
    private UserService userService;
    private EntityManager entityManager;
    private ExerciseSessionService exerciseSessionService;

    @BeforeEach
    void setUp() {

        exerciseSessionRepository =
                mock(ExerciseSessionRepository.class);

        userService =
                mock(UserService.class);

        entityManager =
                mock(EntityManager.class);

        exerciseSessionService =
                new ExerciseSessionService(
                        exerciseSessionRepository,
                        userService,
                        entityManager
                );
    }

    @Test
    void creatingSessionDoesNotChargeUsage() {

        Long userId = 1L;
        User user = createUser();

        when(userService.getUserOrThrow(userId))
                .thenReturn(user);

        when(
                exerciseSessionRepository.save(
                        org.mockito.ArgumentMatchers
                                .any(ExerciseSession.class)
                )
        ).thenAnswer(invocation ->
                invocation.getArgument(0)
        );

        ExerciseSessionResponse response =
                exerciseSessionService.createSession(
                        userId,
                        "스쿼트"
                );

        assertEquals(
                ExerciseSessionStatus.CREATED.name(),
                response.status()
        );

        assertEquals(
                0,
                user.getSetsUsedToday()
        );

        assertEquals(
                1,
                user.getRetakeTickets()
        );

        assertFalse(
                response.usageCharged()
        );
    }

    @Test
    void startingFreeSessionChargesOneFreeUsage() {

        Long userId = 1L;
        User user = createUser();

        ExerciseSession session =
                ExerciseSession.create(
                        user,
                        "스쿼트"
                );

        prepareLockedSession(
                userId,
                session
        );

        ExerciseSessionResponse response =
                exerciseSessionService.startSession(
                        userId,
                        session.getSessionId()
                );

        verify(entityManager)
                .lock(
                        user,
                        LockModeType.PESSIMISTIC_WRITE
                );

        assertEquals(
                ExerciseSessionStatus.STARTED.name(),
                response.status()
        );

        assertEquals(
                1,
                user.getSetsUsedToday()
        );

        assertEquals(
                1,
                user.getRetakeTickets()
        );

        assertTrue(
                response.usageCharged()
        );

        assertTrue(
                response.rewardEligible()
        );

        assertFalse(
                response.ticketUsed()
        );
    }

    @Test
    void duplicateStartDoesNotChargeUsageTwice() {

        Long userId = 1L;
        User user = createUser();

        ExerciseSession session =
                ExerciseSession.create(
                        user,
                        "스쿼트"
                );

        prepareLockedSession(
                userId,
                session
        );

        /*
         * 첫 번째 요청은 정상적으로 시작됩니다.
         */
        exerciseSessionService.startSession(
                userId,
                session.getSessionId()
        );

        assertEquals(
                1,
                user.getSetsUsedToday()
        );

        assertEquals(
                1,
                user.getRetakeTickets()
        );

        /*
         * 같은 세션으로 두 번째 시작 요청을 보내면
         * BusinessException이 발생해야 합니다.
         */
        assertThrows(
                BusinessException.class,
                () -> exerciseSessionService.startSession(
                        userId,
                        session.getSessionId()
                )
        );

        /*
         * 무료 횟수는 한 번만 차감돼야 합니다.
         */
        assertEquals(
                1,
                user.getSetsUsedToday()
        );

        /*
         * 무료 운동이므로 티켓은 그대로 유지돼야 합니다.
         */
        assertEquals(
                1,
                user.getRetakeTickets()
        );

        assertEquals(
                ExerciseSessionStatus.STARTED,
                session.getStatus()
        );

        /*
         * 사용자 DB 잠금도 첫 번째 요청에서만 실행됩니다.
         * 두 번째 요청은 상태 검사에서 바로 차단됩니다.
         */
        verify(
                entityManager,
                times(1)
        ).lock(
                user,
                LockModeType.PESSIMISTIC_WRITE
        );
    }

    @Test
    void startingSessionHasNoDailyLimitAndDoesNotTouchTickets() {

        Long userId = 1L;
        User user = createUser();

        /*
         * 예전 하루 3세트 고정 한도를 이미 넘긴 상태를 가정해도
         * 세션 시작이 차단되지 않아야 합니다.
         */
        user.setSetsUsedToday(10);

        ExerciseSession session =
                ExerciseSession.create(
                        user,
                        "스쿼트"
                );

        prepareLockedSession(
                userId,
                session
        );

        ExerciseSessionResponse response =
                exerciseSessionService.startSession(
                        userId,
                        session.getSessionId()
                );

        assertEquals(
                ExerciseSessionStatus.STARTED.name(),
                response.status()
        );

        assertEquals(
                11,
                user.getSetsUsedToday()
        );

        assertEquals(
                1,
                user.getRetakeTickets()
        );

        assertTrue(
                response.usageCharged()
        );

        assertTrue(
                response.rewardEligible()
        );

        assertFalse(
                response.ticketUsed()
        );
    }

    @Test
    void failureBeforeStartDoesNotChargeUsage() {

        Long userId = 1L;
        User user = createUser();

        ExerciseSession session =
                ExerciseSession.create(
                        user,
                        "스쿼트"
                );

        prepareLockedSession(
                userId,
                session
        );

        ExerciseSessionResponse response =
                exerciseSessionService.failSession(
                        userId,
                        session.getSessionId(),
                        "카메라 권한이 거부되었습니다."
                );

        assertEquals(
                ExerciseSessionStatus.FAILED.name(),
                response.status()
        );

        assertEquals(
                0,
                user.getSetsUsedToday()
        );

        assertEquals(
                1,
                user.getRetakeTickets()
        );

        assertFalse(
                response.usageCharged()
        );

        assertEquals(
                "카메라 권한이 거부되었습니다.",
                response.failureReason()
        );
    }

    @Test
    void abortAfterStartKeepsChargedUsage() {

        Long userId = 1L;
        User user = createUser();

        user.setSetsUsedToday(1);

        ExerciseSession session =
                ExerciseSession.create(
                        user,
                        "스쿼트"
                );

        session.configureFreeExercise();
        session.markStarted();

        prepareLockedSession(
                userId,
                session
        );

        ExerciseSessionResponse response =
                exerciseSessionService.abortSession(
                        userId,
                        session.getSessionId()
                );

        assertEquals(
                ExerciseSessionStatus.ABORTED.name(),
                response.status()
        );

        assertEquals(
                1,
                user.getSetsUsedToday()
        );

        assertTrue(
                response.usageCharged()
        );
    }

    /**
     * 잠금 조회에서 사용할 세션을 설정합니다.
     */
    private void prepareLockedSession(
            Long userId,
            ExerciseSession session
    ) {

        when(
                exerciseSessionRepository
                        .findBySessionIdAndUserIdForUpdate(
                                session.getSessionId(),
                                userId
                        )
        ).thenReturn(
                Optional.of(session)
        );
    }

    /**
     * 테스트용 사용자입니다.
     */
    private User createUser() {

        User user =
                User.register(
                        "session-test-user",
                        "encoded-password",
                        "session-test@example.com",
                        "세션테스트",
                        User.Gender.MALE
                );

        user.setLevel(1);
        user.setExp(0);
        user.setPoints(0);
        user.setGrade(UserGrade.IRON);

        user.setSetsUsedToday(0);
        user.setSetsResetDate(LocalDate.now());

        user.setRetakeTickets(1);

        return user;
    }
}
