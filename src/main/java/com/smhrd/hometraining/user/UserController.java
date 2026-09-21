package com.smhrd.hometraining.user;

import com.smhrd.hometraining.common.ApiResponse;
import com.smhrd.hometraining.security.CustomUserPrincipal;
import com.smhrd.hometraining.user.dto.CalibrationRequest;
import com.smhrd.hometraining.user.dto.SocialOnboardingRequest;
import com.smhrd.hometraining.user.dto.UpdateProfileRequest;
import com.smhrd.hometraining.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * [담당] 로그인한 "내" 계정 정보 조회/수정 — 프로필, 소셜 온보딩, 캘리브레이션, 출석, 회원탈퇴.
 * [프론트 연동] ounhome-f/js/auth.js(loadMyProfile), profile.js(계정관리 탭, doWithdraw),
 *              calibration.js(캘리브레이션 저장) 등 다수 파일이 /api/users/me/**를 호출한다.
 * [DB] UserService → users, calibration_profiles 테이블. 회원탈퇴(DELETE)는 CrewService의
 *      크루 정리까지 연쇄로 타는 되돌릴 수 없는 동작이니 로직 변경 시 특히 조심할 것.
 */
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ApiResponse<UserResponse> getMe(@AuthenticationPrincipal CustomUserPrincipal principal) {
        return ApiResponse.ok(userService.getMe(principal.getUserId()));
    }

    @PatchMapping
    public ApiResponse<UserResponse> updateProfile(@AuthenticationPrincipal CustomUserPrincipal principal,
                                                     @RequestBody UpdateProfileRequest req) {
        return ApiResponse.ok(userService.updateProfile(principal.getUserId(), req));
    }

    /**
     * 소셜 로그인으로 처음 가입한 사용자가 닉네임·동네·캐릭터(성별)를 한 번에 확정합니다.
     * (updateProfile과 달리 닉네임 변경권을 쓰지 않음 — SocialOnboardingRequest 주석 참고)
     */
    @PatchMapping("/onboarding")
    public ApiResponse<UserResponse> completeSocialOnboarding(@AuthenticationPrincipal CustomUserPrincipal principal,
                                                                @Valid @RequestBody SocialOnboardingRequest req) {
        return ApiResponse.ok(userService.completeSocialOnboarding(principal.getUserId(), req));
    }

    @GetMapping("/calibration")
    public ApiResponse<Map<String, String>> getCalibration(@AuthenticationPrincipal CustomUserPrincipal principal) {
        String json = userService.getCalibrationProfileJson(principal.getUserId());
        return ApiResponse.ok(json == null ? Map.of() : Map.of("profileJson", json));
    }

    @PutMapping("/calibration")
    public ApiResponse<Void> saveCalibration(@AuthenticationPrincipal CustomUserPrincipal principal,
                                              @Valid @RequestBody CalibrationRequest req) {
        userService.saveCalibration(principal.getUserId(), req);
        return ApiResponse.ok();
    }

    @PostMapping("/streak/claim")
    public ApiResponse<Map<String, Long>> claimStreak(@AuthenticationPrincipal CustomUserPrincipal principal) {
        return ApiResponse.ok(Map.of("pointsAwarded", userService.claimStreakReward(principal.getUserId())));
    }

    @DeleteMapping
    public ApiResponse<Void> withdraw(@AuthenticationPrincipal CustomUserPrincipal principal) {
        userService.withdraw(principal.getUserId());
        return ApiResponse.ok();
    }
}
