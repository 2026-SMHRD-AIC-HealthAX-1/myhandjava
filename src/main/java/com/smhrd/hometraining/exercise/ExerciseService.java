package com.smhrd.hometraining.exercise;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smhrd.hometraining.common.exception.BusinessException;
import com.smhrd.hometraining.exercise.dto.ExerciseRecordResponse;
import com.smhrd.hometraining.exercise.dto.ExerciseResultRequest;
import com.smhrd.hometraining.exercise.entity.ExerciseRecord;
import com.smhrd.hometraining.exercise.policy.ExerciseExpPolicy;
import com.smhrd.hometraining.exercise.policy.ExercisePointPolicy;
import com.smhrd.hometraining.exercise.policy.ExerciseScorePolicy;
import com.smhrd.hometraining.exercise.repository.ExerciseRecordRepository;
import com.smhrd.hometraining.exercise.session.ExerciseSessionService;
import com.smhrd.hometraining.exercise.session.entity.ExerciseSession;
import com.smhrd.hometraining.mission.MissionService;
import com.smhrd.hometraining.user.UserService;
import com.smhrd.hometraining.user.entity.User;
import com.smhrd.hometraining.user.entity.UserResourceHistory.Reason;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExerciseService {

    private final ExerciseRecordRepository exerciseRecordRepository;
    private final UserService userService;
    private final MissionService missionService;
    private final ExerciseSessionService exerciseSessionService;

    /**
     * 운동 결과를 검증하고 저장합니다.
     */
    @Transactional
    public ExerciseRecordResponse saveResult(
            Long userId,
            ExerciseResultRequest req
    ) {

        /*
         * 동일 세션의 중복 요청을 막기 위해
         * DB 잠금 상태로 세션을 조회합니다.
         */
        ExerciseSession session =
                exerciseSessionService
                        .getSessionEntityForUpdate(
                                userId,
                                req.sessionId()
                        );

        ExerciseRecord existingRecord =
                exerciseRecordRepository
                        .findBySessionSessionIdAndUserId(
                                req.sessionId(),
                                userId
                        )
                        .orElse(null);

        if (existingRecord != null) {
            if (req.idempotencyKey().equals(
                    existingRecord.getIdempotencyKey()
            )) {
                return ExerciseRecordResponse.from(existingRecord);
            }

            throw new BusinessException(
                    "이미 다른 중복 방지 키로 저장된 운동 세션입니다."
            );
        }

        if (exerciseRecordRepository
                .findByUserIdAndIdempotencyKey(
                        userId,
                        req.idempotencyKey()
                )
                .isPresent()) {
            throw new BusinessException(
                    "이미 사용된 중복 방지 키입니다."
            );
        }

        validateSessionForResult(
                session,
                req
        );

        /*
         * 전체 운동 횟수와 자세 판정 횟수의
         * 합계가 같은지 검사합니다.
         */
        ExerciseScorePolicy.validateRepsMatch(
                req.reps(),
                req.perfectCount(),
                req.greatCount(),
                req.goodCount(),
                req.missCount()
        );

        /*
         * 동일 세션 결과가 이미 저장됐다면
         * 중복 저장과 중복 보상을 차단합니다.
         */
        if (exerciseRecordRepository
                .existsBySessionSessionId(
                        req.sessionId()
                )) {

            throw new BusinessException(
                    "이미 결과가 저장된 운동 세션입니다."
            );
        }

        /*
         * 프론트에서 전달한 req.score()는 사용하지 않습니다.
         *
         * 자세 판정 횟수로 서버가 점수를 계산합니다.
         */
        int calculatedScore =
                ExerciseScorePolicy.calculateScore(
                        req.perfectCount(),
                        req.greatCount(),
                        req.goodCount(),
                        req.missCount()
                );

        /*
         * 보상은 요청의 sessionType 등급에 따라 차등 지급됩니다.
         *
         * FREE: 정상 지급. REDUCED: 포인트 미지급, 경험치는 1/3만 지급
         * (하루 6번째 운동부터 이 등급으로 옴). RANKED: 포인트·경험치 모두
         * 미지급(순위 도전 모드 — 점수만 기록).
         */
        int fullExpAwarded =
                ExerciseExpPolicy.calculateExp(
                        calculatedScore
                );

        int fullPointsAwarded =
                ExercisePointPolicy.calculatePoints(
                        calculatedScore
                );

        int expAwarded;
        int pointsAwarded;

        switch (req.sessionType()) {

            case FREE -> {
                expAwarded = fullExpAwarded;
                pointsAwarded = fullPointsAwarded;
            }

            case REDUCED -> {
                expAwarded = fullExpAwarded / 3;
                pointsAwarded = 0;
            }

            case RANKED -> {
                expAwarded = 0;
                pointsAwarded = 0;
            }

            default -> throw new BusinessException(
                    "알 수 없는 세션 보상 등급입니다."
            );
        }

        User user =
                session.getUser();

        /*
         * 운동 기록에 실제 지급 경험치와 포인트를
         * 모두 저장합니다.
         */
        ExerciseRecord record =
                ExerciseRecord.create(
                        user,
                        session,
                        req.idempotencyKey(),
                        session.getExerciseType(),
                        req.reps(),
                        req.accuracy(),
                        calculatedScore,
                        req.perfectCount(),
                        req.greatCount(),
                        req.goodCount(),
                        req.missCount(),
                        expAwarded,
                        pointsAwarded,
                        ExerciseRecord.SessionType.valueOf(
                                req.sessionType().name()
                        )
                );

        /*
         * DB의 session_id UNIQUE 검사를
         * 보상 지급 전에 즉시 실행합니다.
         */
        exerciseRecordRepository.saveAndFlush(
                record
        );

        /*
         * 기록 저장에 성공한 후
         * 운동 세션을 완료 처리합니다.
         */
        session.markCompleted();

        /*
         * 순위 도전 세션은 exp/포인트 보상은 없지만, 점수는 사용자의 순위 도전
         * 누적 점수에 반영되어 등급(아이언~챌린저)을 올리는 데 쓰입니다.
         */
        if (req.sessionType() == ExerciseResultRequest.SessionType.RANKED) {

            user.recordRankedChallengeScore(
                    calculatedScore
            );
        }

        /*
         * REDUCED 세션이 경험치 1/3 계산에서 0으로 내려갈 수도 있고,
         * RANKED 세션은 항상 0이므로, 실제 지급할 보상이 있을 때만
         * 재화 변동 이력을 남깁니다.
         */
        if (expAwarded > 0 || pointsAwarded > 0) {

            userService.grantRewards(
                    user,
                    expAwarded,
                    pointsAwarded,
                    Reason.EXERCISE_REWARD,
                    session.getSessionId()
            );
        }

        /*
         * 일일 미션 진행도는 무료 운동과
         * 다시찍기 티켓 운동 모두 반영합니다.
         *
         * 운동 결과가 정상적으로 저장된
         * 스쿼트 세션이면 이용 방식과 관계없이
         * 미션 카운터를 증가시킵니다.
         */
        if ("스쿼트".equals(
                session.getExerciseType()
        )) {

            missionService.recordSquatSession(
                    userId,
                    req.reps(),
                    req.perfectCount(),
                    req.missCount(),
                    req.accuracy()
            );
        } else {
            missionService.recordExerciseSession(
                    userId,
                    session.getExerciseType(),
                    req.reps(),
                    req.perfectCount(),
                    req.missCount(),
                    req.accuracy()
            );
        }

        return ExerciseRecordResponse.from(record);
    }

    /**
     * 운동 결과를 저장할 수 있는 세션인지 검사합니다.
     */
    private void validateSessionForResult(
            ExerciseSession session,
            ExerciseResultRequest req
    ) {

        if (session.isResultSaved()) {
            throw new BusinessException(
                    "이미 결과가 저장된 운동 세션입니다."
            );
        }

        if (!session.getStatus().canComplete()) {
            throw new BusinessException(
                    "운동 중인 세션만 결과를 저장할 수 있습니다."
            );
        }

        if (req.exerciseType() == null
                || !session.getExerciseType().equals(
                        req.exerciseType().trim()
                )) {

            throw new BusinessException(
                    "운동 세션의 운동 종류와 결과의 운동 종류가 일치하지 않습니다."
            );
        }

        /*
         * 한 세트는 15회 달성 또는
         * 운동 시작 후 120초 경과 시 종료됩니다.
         */
        if (req.reps() < ExerciseScorePolicy.MAX_REPS
                && !session.hasExceededTimeLimit(
                        LocalDateTime.now()
                )) {

            throw new BusinessException(
                    "15회 달성 또는 120초 경과 후 운동 결과를 저장할 수 있습니다."
            );
        }
    }

    /**
     * 사용자의 운동 기록을 최신순으로 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<ExerciseRecordResponse> getHistory(
            Long userId
    ) {

        return exerciseRecordRepository
                .findByUserIdOrderByRecordedAtDesc(
                        userId
                )
                .stream()
                .map(ExerciseRecordResponse::from)
                .toList();
    }
}
