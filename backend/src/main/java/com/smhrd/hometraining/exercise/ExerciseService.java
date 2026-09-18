package com.smhrd.hometraining.exercise;

import com.smhrd.hometraining.common.exception.BusinessException;
import com.smhrd.hometraining.exercise.dto.ExerciseRecordResponse;
import com.smhrd.hometraining.exercise.dto.ExerciseResultRequest;
import com.smhrd.hometraining.exercise.entity.ExerciseRecord;
import com.smhrd.hometraining.exercise.repository.ExerciseRecordRepository;
import com.smhrd.hometraining.mission.MissionService;
import com.smhrd.hometraining.user.UserService;
import com.smhrd.hometraining.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExerciseService {

    private final ExerciseRecordRepository exerciseRecordRepository;
    private final UserService userService;
    private final MissionService missionService;

    @Transactional
    public ExerciseRecordResponse saveResult(Long userId, ExerciseResultRequest req) {
        User user = userService.getUserOrThrow(userId);
        user.resetDailySetsIfNeeded();
        if (user.getSetsUsedToday() >= user.getDailySetLimit()) {
            throw new BusinessException("오늘 가능한 운동세트를 모두 사용했습니다.");
        }

        int pointsAwarded = Math.round(req.score() * 0.4f);
        ExerciseRecord record = ExerciseRecord.create(
                user, req.exerciseType(), req.reps(), req.accuracy(), req.score(),
                req.perfectCount(), req.greatCount(), req.goodCount(), req.missCount(),
                pointsAwarded
        );
        exerciseRecordRepository.save(record);

        user.setSetsUsedToday(user.getSetsUsedToday() + 1);
        // 경험치는 획득 포인트와 동일하게 지급한다 (script.js 목업은 exp를 갱신하지 않는 정적 값이었음).
        userService.grantRewards(user, pointsAwarded, pointsAwarded);

        if ("스쿼트".equals(req.exerciseType())) {
            missionService.recordSquatSession(userId, req.reps(), req.perfectCount(), req.missCount(), req.accuracy());
        }

        return ExerciseRecordResponse.from(record);
    }

    @Transactional(readOnly = true)
    public List<ExerciseRecordResponse> getHistory(Long userId) {
        return exerciseRecordRepository.findByUserIdOrderByRecordedAtDesc(userId).stream()
                .map(ExerciseRecordResponse::from).toList();
    }
}
