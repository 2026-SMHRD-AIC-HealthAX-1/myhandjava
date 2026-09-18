package com.smhrd.hometraining.exercise.session;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smhrd.hometraining.common.ApiResponse;
import com.smhrd.hometraining.exercise.session.dto.CreateExerciseSessionRequest;
import com.smhrd.hometraining.exercise.session.dto.DailyExerciseStatusResponse;
import com.smhrd.hometraining.exercise.session.dto.ExerciseSessionResponse;
import com.smhrd.hometraining.exercise.session.dto.FailExerciseSessionRequest;
import com.smhrd.hometraining.security.CustomUserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/exercise-sessions")
@RequiredArgsConstructor
public class ExerciseSessionController {

    private final ExerciseSessionService exerciseSessionService;

    /**
     * 오늘 무료 운동 이용 현황을 조회합니다.
     *
     * 하루 무료 운동 한도,
     * 오늘 사용한 무료 운동 횟수,
     * 남은 무료 운동 횟수,
     * 보유 재도전권 개수를 반환합니다.
     */
    @GetMapping("/daily-status")
    public ApiResponse<DailyExerciseStatusResponse> getDailyExerciseStatus(
            @AuthenticationPrincipal
            CustomUserPrincipal principal
    ) {

        DailyExerciseStatusResponse response =
                exerciseSessionService.getDailyExerciseStatus(
                        principal.getUserId()
                );

        return ApiResponse.ok(response);
    }

    /**
     * 새로운 운동 세션을 생성합니다.
     *
     * 아직 카메라와 자세 인식이 준비되지 않은 단계이므로
     * 무료 운동 횟수나 티켓을 차감하지 않습니다.
     */
    @PostMapping
    public ApiResponse<ExerciseSessionResponse> createSession(
            @AuthenticationPrincipal
            CustomUserPrincipal principal,

            @Valid
            @RequestBody
            CreateExerciseSessionRequest request
    ) {

        ExerciseSessionResponse response =
                exerciseSessionService.createSession(
                        principal.getUserId(),
                        request.exerciseType()
                );

        return ApiResponse.ok(response);
    }

    /**
     * 카메라와 자세 인식이 정상적으로 준비된 후
     * 실제 운동 시작을 처리합니다.
     *
     * 이 요청이 성공하는 시점에
     * 무료 횟수 또는 티켓이 차감됩니다.
     */
    @PostMapping("/{sessionId}/start")
    public ApiResponse<ExerciseSessionResponse> startSession(
            @AuthenticationPrincipal
            CustomUserPrincipal principal,

            @PathVariable
            String sessionId
    ) {

        ExerciseSessionResponse response =
                exerciseSessionService.startSession(
                        principal.getUserId(),
                        sessionId
                );

        return ApiResponse.ok(response);
    }

    /**
     * 카메라 권한 거부, 연결 실패,
     * 자세 인식 초기화 실패를 처리합니다.
     *
     * 운동 시작 전 실패이므로
     * 무료 횟수나 티켓을 차감하지 않습니다.
     */
    @PostMapping("/{sessionId}/fail")
    public ApiResponse<ExerciseSessionResponse> failSession(
            @AuthenticationPrincipal
            CustomUserPrincipal principal,

            @PathVariable
            String sessionId,

            @Valid
            @RequestBody
            FailExerciseSessionRequest request
    ) {

        ExerciseSessionResponse response =
                exerciseSessionService.failSession(
                        principal.getUserId(),
                        sessionId,
                        request.reason()
                );

        return ApiResponse.ok(response);
    }

    /**
     * 정상 시작 후 사용자가 운동 화면에서
     * 중간에 나간 경우를 처리합니다.
     *
     * 이미 차감된 운동 횟수나 티켓은
     * 복구하지 않습니다.
     */
    @PostMapping("/{sessionId}/abort")
    public ApiResponse<ExerciseSessionResponse> abortSession(
            @AuthenticationPrincipal
            CustomUserPrincipal principal,

            @PathVariable
            String sessionId
    ) {

        ExerciseSessionResponse response =
                exerciseSessionService.abortSession(
                        principal.getUserId(),
                        sessionId
                );

        return ApiResponse.ok(response);
    }

    /**
     * 본인의 운동 세션 한 건을 조회합니다.
     */
    @GetMapping("/{sessionId}")
    public ApiResponse<ExerciseSessionResponse> getSession(
            @AuthenticationPrincipal
            CustomUserPrincipal principal,

            @PathVariable
            String sessionId
    ) {

        ExerciseSessionResponse response =
                exerciseSessionService.getSession(
                        principal.getUserId(),
                        sessionId
                );

        return ApiResponse.ok(response);
    }

    /**
     * 본인의 운동 세션 내역을 최신순으로 조회합니다.
     */
    @GetMapping
    public ApiResponse<List<ExerciseSessionResponse>> getSessionHistory(
            @AuthenticationPrincipal
            CustomUserPrincipal principal
    ) {

        List<ExerciseSessionResponse> response =
                exerciseSessionService.getSessionHistory(
                        principal.getUserId()
                );

        return ApiResponse.ok(response);
    }
}