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

/**
 * [담당] 운동 "세션"(시작~종료) 생명주기 관리 — 오늘 무료 운동 가능 횟수 확인도 여기.
 * [프론트 연동] ounhome-f/js/exercise.js — 카메라 켤 때 POST(생성)/PATCH .../start,
 *              중간에 실패하면 .../fail, .../abort. 결과 저장(POST /api/exercise-records)은
 *              이 컨트롤러가 아니라 ExerciseController가 담당 — 세션과 결과는 분리된 API다.
 * [DB] ExerciseSessionService → exercise_sessions 테이블(status로 생성/시작/완료/실패/중단 추적).
 * [주의] 세션 생성 직후 바로 결과를 저장하려 하면 트랜잭션 타이밍 상 실패할 수 있어, 프론트에서
 *        재시도 로직(exercise.js saveExerciseResult)으로 방어하고 있다 — 여기 상태 전이 규칙을
 *        바꾸면 그 재시도 로직도 같이 확인할 것.
 */
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