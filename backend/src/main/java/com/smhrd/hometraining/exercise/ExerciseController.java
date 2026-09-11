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
