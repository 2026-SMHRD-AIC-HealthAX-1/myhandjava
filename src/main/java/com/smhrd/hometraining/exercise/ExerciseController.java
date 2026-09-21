package com.smhrd.hometraining.exercise;

import com.smhrd.hometraining.common.ApiResponse;
import com.smhrd.hometraining.exercise.dto.ExerciseRecordResponse;
import com.smhrd.hometraining.exercise.dto.ExerciseResultRequest;
import com.smhrd.hometraining.exercise.entity.ExerciseDefinition;
import com.smhrd.hometraining.exercise.repository.ExerciseDefinitionRepository;
import com.smhrd.hometraining.security.CustomUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * [담당] 운동 종목 카탈로그 조회 + 운동 결과 저장(캘리브레이션 이후 최종 기록).
 * [프론트 연동] ounhome-f/js/exercise.js — GET /api/exercises(종목 목록),
 *              POST /api/exercise-records(saveExerciseResult), GET /api/exercise-records(히스토리).
 * [DB] ⚠️ catalog()는 서비스를 거치지 않고 exerciseDefinitionRepository를 컨트롤러에서 직접
 *      호출한다(단순 조회라 예외적으로 허용된 패턴) — exercise_definitions 테이블.
 *      결과 저장은 ExerciseService → exercise_records/exercise_sessions/missions 등 연쇄 갱신.
 */
@RestController
@RequiredArgsConstructor
public class ExerciseController {

    private final ExerciseService exerciseService;
    private final ExerciseDefinitionRepository exerciseDefinitionRepository;

    @GetMapping("/api/exercises")
    public ApiResponse<List<ExerciseDefinition>> catalog() {
        return ApiResponse.ok(exerciseDefinitionRepository.findAll());
    }

    @PostMapping("/api/exercise-records")
    public ApiResponse<ExerciseRecordResponse> save(@AuthenticationPrincipal CustomUserPrincipal principal,
                                                      @Valid @RequestBody ExerciseResultRequest req) {
        return ApiResponse.ok(exerciseService.saveResult(principal.getUserId(), req));
    }

    @GetMapping("/api/exercise-records")
    public ApiResponse<List<ExerciseRecordResponse>> history(@AuthenticationPrincipal CustomUserPrincipal principal) {
        return ApiResponse.ok(exerciseService.getHistory(principal.getUserId()));
    }
}
