package com.smhrd.hometraining.exercise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.smhrd.hometraining.common.exception.BusinessException;
import com.smhrd.hometraining.exercise.dto.ExerciseRecordResponse;
import com.smhrd.hometraining.exercise.dto.ExerciseResultRequest;
import com.smhrd.hometraining.exercise.entity.ExerciseRecord;
import com.smhrd.hometraining.exercise.repository.ExerciseRecordRepository;
import com.smhrd.hometraining.exercise.session.ExerciseSessionService;
import com.smhrd.hometraining.exercise.session.entity.ExerciseSession;
import com.smhrd.hometraining.exercise.session.entity.ExerciseSessionStatus;
import com.smhrd.hometraining.mission.MissionService;
import com.smhrd.hometraining.user.UserService;
import com.smhrd.hometraining.user.entity.User;
import com.smhrd.hometraining.user.entity.UserGrade;
import com.smhrd.hometraining.user.entity.UserResourceHistory.Reason;

class ExerciseServiceTest {

    private ExerciseRecordRepository exerciseRecordRepository;
    private UserService userService;
    private MissionService missionService;
    private ExerciseSessionService exerciseSessionService;
    private ExerciseService exerciseService;

    @BeforeEach
    void setUp() {

        exerciseRecordRepository =
                mock(ExerciseRecordRepository.class);

        userService =
                mock(UserService.class);

        missionService =
                mock(MissionService.class);

        exerciseSessionService =
                mock(ExerciseSessionService.class);

        exerciseService =
                new ExerciseService(
                        exerciseRecordRepository,
                        userService,
                        missionService,
                        exerciseSessionService
                );
    }

    @Test
    void freeExerciseStoresAndGrantsRewards() {

        Long userId = 1L;
        User user = createUser();

        ExerciseSession session =
                createFreeStartedSession(user);

        String sessionId =
                session.getSessionId();

        prepareSession(
                userId,
                session
        );

        ExerciseResultRequest request =
                createPerfectRequest(sessionId);

        ExerciseRecordResponse response =
                exerciseService.saveResult(
                        userId,
                        request
                );

        /*
         * 프론트에서 보낸 점수는 0점이지만
         * PERFECT 15회로 서버가 1,500점을 계산합니다.
         */
        assertEquals(
                1500,
                response.score()
        );

        assertEquals(
                500,
                response.expAwarded()
        );

        assertEquals(
                600,
                response.pointsAwarded()
        );

        assertTrue(
                response.rewardEligible()
        );

        assertFalse(
                response.ticketUsed()
        );

        assertEquals(
                sessionId,
                response.sessionId()
        );

        ArgumentCaptor<ExerciseRecord> recordCaptor =
                ArgumentCaptor.forClass(
                        ExerciseRecord.class
                );

        verify(exerciseRecordRepository)
                .saveAndFlush(
                        recordCaptor.capture()
                );

        ExerciseRecord savedRecord =
                recordCaptor.getValue();

        assertEquals(
                sessionId,
                savedRecord
                        .getSession()
                        .getSessionId()
        );

        assertEquals(
                1500,
                savedRecord.getScore()
        );

        assertEquals(
                500,
                savedRecord.getExpAwarded()
        );

        assertEquals(
                600,
                savedRecord.getPointsAwarded()
        );

        verify(userService)
                .grantRewards(
                        user,
                        500,
                        600,
                        Reason.EXERCISE_REWARD,
                        sessionId
                );

        verify(missionService)
                .recordSquatSession(
                        userId,
                        15,
                        15,
                        0,
                        100
                );

        assertEquals(
                ExerciseSessionStatus.COMPLETED,
                session.getStatus()
        );

        assertTrue(
                session.isResultSaved()
        );

        /*
         * 사용 횟수는 세션 시작 시 이미 차감됐으므로
         * 결과 저장에서는 추가로 증가하지 않습니다.
         */
        assertEquals(
                1,
                user.getSetsUsedToday()
        );

        /*
         * 자유 운동(FREE)은 순위 도전 누적 점수에 반영되지 않습니다.
         */
        assertEquals(
                0,
                user.getRankedScoreTotal()
        );
    }

    @Test
    void rankedExerciseStoresScoreWithoutRewards() {

        Long userId = 1L;
        User user = createUser();

        ExerciseSession session =
                createFreeStartedSession(user);

        String sessionId =
                session.getSessionId();

        prepareSession(
                userId,
                session
        );

        ExerciseResultRequest request =
                createPerfectRequest(
                        sessionId,
                        ExerciseResultRequest.SessionType.RANKED
                );

        ExerciseRecordResponse response =
                exerciseService.saveResult(
                        userId,
                        request
                );

        /*
         * 순위 도전 세션도 점수와 기록은 정상 저장됩니다.
         */
        assertEquals(
                1500,
                response.score()
        );

        /*
         * 순위 도전 세션은 경험치와 포인트를 지급하지 않습니다.
         */
        assertEquals(
                0,
                response.expAwarded()
        );

        assertEquals(
                0,
                response.pointsAwarded()
        );

        assertFalse(
                response.rewardEligible()
        );

        /*
         * 사용자 보상 지급이 호출되면 안 됩니다.
         */
        verify(
                userService,
                never()
        ).grantRewards(
                any(User.class),
                anyInt(),
                anyLong()
        );

        assertEquals(
                ExerciseSessionStatus.COMPLETED,
                session.getStatus()
        );

        /*
         * 보상은 없지만 순위 도전 누적 점수에는 반영되어
         * 첫 참여만으로 아이언 등급이 부여됩니다.
         */
        assertEquals(
                1500,
                user.getRankedScoreTotal()
        );

        assertEquals(
                UserGrade.IRON,
                user.getGrade()
        );
    }

    @Test
    void reducedExerciseGrantsOneThirdExpAndNoPoints() {

        Long userId = 1L;
        User user = createUser();

        ExerciseSession session =
                createFreeStartedSession(user);

        String sessionId =
                session.getSessionId();

        prepareSession(
                userId,
                session
        );

        ExerciseResultRequest request =
                createPerfectRequest(
                        sessionId,
                        ExerciseResultRequest.SessionType.REDUCED
                );

        ExerciseRecordResponse response =
                exerciseService.saveResult(
                        userId,
                        request
                );

        assertEquals(
                1500,
                response.score()
        );

        /*
         * 1,500점 PERFECT의 정상 경험치는 500이므로,
         * REDUCED는 그 1/3인 166만 지급됩니다.
         */
        assertEquals(
                166,
                response.expAwarded()
        );

        assertEquals(
                0,
                response.pointsAwarded()
        );

        verify(userService)
                .grantRewards(
                        user,
                        166,
                        0,
                        Reason.EXERCISE_REWARD,
                        sessionId
                );

        assertEquals(
                ExerciseSessionStatus.COMPLETED,
                session.getStatus()
        );
    }

    @Test
    void duplicateSessionResultIsRejected() {

        Long userId = 1L;
        User user = createUser();

        ExerciseSession session =
                createFreeStartedSession(user);

        String sessionId =
                session.getSessionId();

        when(
                exerciseSessionService
                        .getSessionEntityForUpdate(
                                userId,
                                sessionId
                        )
        ).thenReturn(session);

        when(
                exerciseRecordRepository
                        .existsBySessionSessionId(
                                sessionId
                        )
        ).thenReturn(true);

        ExerciseResultRequest request =
                createPerfectRequest(sessionId);

        assertThrows(
                BusinessException.class,
                () -> exerciseService.saveResult(
                        userId,
                        request
                )
        );

        verify(
                exerciseRecordRepository,
                never()
        ).saveAndFlush(
                any(ExerciseRecord.class)
        );

        verify(
                userService,
                never()
        ).grantRewards(
                any(User.class),
                anyInt(),
                anyLong()
        );

        assertEquals(
                ExerciseSessionStatus.STARTED,
                session.getStatus()
        );
    }

    @Test
    void sameIdempotencyKeyReturnsExistingResultWithoutRewardingAgain() {

        Long userId = 1L;
        User user = createUser();
        ExerciseSession session = createFreeStartedSession(user);
        ExerciseResultRequest request =
                createPerfectRequest(session.getSessionId());

        ExerciseRecord existing =
                ExerciseRecord.create(
                        user,
                        session,
                        request.idempotencyKey(),
                        session.getExerciseType(),
                        15,
                        100,
                        1500,
                        15,
                        0,
                        0,
                        0,
                        500,
                        600,
                        ExerciseRecord.SessionType.FREE
                );

        when(
                exerciseSessionService.getSessionEntityForUpdate(
                        userId,
                        session.getSessionId()
                )
        ).thenReturn(session);

        when(
                exerciseRecordRepository
                        .findBySessionSessionIdAndUserId(
                                session.getSessionId(),
                                userId
                        )
        ).thenReturn(Optional.of(existing));

        ExerciseRecordResponse response =
                exerciseService.saveResult(userId, request);

        assertEquals(1500, response.score());

        verify(exerciseRecordRepository, never())
                .saveAndFlush(any(ExerciseRecord.class));

        verify(userService, never())
                .grantRewards(
                        any(User.class),
                        anyInt(),
                        anyLong(),
                        any(Reason.class),
                        any()
                );
    }

    @Test
    void mismatchedRepsAreRejectedBeforeSaving() {

        Long userId = 1L;
        User user = createUser();

        ExerciseSession session =
                createFreeStartedSession(user);

        String sessionId =
                session.getSessionId();

        when(
                exerciseSessionService
                        .getSessionEntityForUpdate(
                                userId,
                                sessionId
                        )
        ).thenReturn(session);

        /*
         * reps는 15지만 자세 판정 합계는 14입니다.
         */
        ExerciseResultRequest request =
                new ExerciseResultRequest(
                        sessionId,
                        "스쿼트",
                        15,
                        90,
                        1500,
                        10,
                        2,
                        1,
                        1
                );

        assertThrows(
                BusinessException.class,
                () -> exerciseService.saveResult(
                        userId,
                        request
                )
        );

        verify(
                exerciseRecordRepository,
                never()
        ).saveAndFlush(
                any(ExerciseRecord.class)
        );

        verify(
                userService,
                never()
        ).grantRewards(
                any(User.class),
                anyInt(),
                anyLong()
        );

        verify(
                missionService,
                never()
        ).recordSquatSession(
                anyLong(),
                anyInt(),
                anyInt(),
                anyInt(),
                anyInt()
        );

        assertEquals(
                ExerciseSessionStatus.STARTED,
                session.getStatus()
        );
    }

    /**
     * 테스트에서 공통으로 사용하는 Mock 동작입니다.
     */
    private void prepareSession(
            Long userId,
            ExerciseSession session
    ) {

        when(
                exerciseSessionService
                        .getSessionEntityForUpdate(
                                userId,
                                session.getSessionId()
                        )
        ).thenReturn(session);

        when(
                exerciseRecordRepository
                        .existsBySessionSessionId(
                                session.getSessionId()
                        )
        ).thenReturn(false);

        when(
                exerciseRecordRepository
                        .saveAndFlush(
                                any(ExerciseRecord.class)
                        )
        ).thenAnswer(invocation ->
                invocation.getArgument(0)
        );
    }

    /**
     * PERFECT 15회 요청입니다.
     *
     * 프론트 점수는 0으로 보내 서버가
     * 점수를 다시 계산하는지 확인합니다.
     */
    private ExerciseResultRequest createPerfectRequest(
            String sessionId
    ) {

        return new ExerciseResultRequest(
                sessionId,
                "스쿼트",
                15,
                100,
                0,
                15,
                0,
                0,
                0
        );
    }

    /**
     * sessionType을 직접 지정하는 PERFECT 15회 요청입니다.
     */
    private ExerciseResultRequest createPerfectRequest(
            String sessionId,
            ExerciseResultRequest.SessionType sessionType
    ) {

        return new ExerciseResultRequest(
                sessionId,
                java.util.UUID.randomUUID().toString(),
                "스쿼트",
                15,
                100,
                0,
                15,
                0,
                0,
                0,
                sessionType
        );
    }

    /**
     * 무료 운동 STARTED 세션입니다.
     */
    private ExerciseSession createFreeStartedSession(
            User user
    ) {

        ExerciseSession session =
                ExerciseSession.create(
                        user,
                        "스쿼트"
                );

        session.configureFreeExercise();
        session.markStarted();

        return session;
    }

    /**
     * 테스트용 사용자입니다.
     */
    private User createUser() {

        User user =
                User.register(
                        "exercise-test-user",
                        "encoded-password",
                        "exercise-test@example.com",
                        "운동테스트",
                        User.Gender.MALE
                );

        user.setLevel(1);
        user.setExp(0);
        user.setPoints(0);
        user.setGrade(UserGrade.IRON);

        user.setSetsUsedToday(1);
        user.setSetsResetDate(LocalDate.now());

        return user;
    }
}
