package com.smhrd.hometraining.auth;

import com.smhrd.hometraining.auth.dto.*;
import com.smhrd.hometraining.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * [담당] 로그인/소셜 로그인/토큰 발급 — 회원가입(signup)·아이디비번 로그인(login)은 프론트에서
 *        더 이상 안 부르지만(SNS 전용으로 전환), 백엔드는 그대로 살아있다.
 * [프론트 연동] ounhome-f/js/auth.js — doSocialLogin()이 카카오/구글로 리다이렉트한 뒤
 *              handleKakaoRedirect()/handleGoogleRedirect()가 POST /api/auth/kakao/login,
 *              /api/auth/google/login을 호출한다.
 * [DB] 여기서 직접 쿼리하지 않고 AuthService → UserRepository → users 테이블.
 * [주의] 로그인 성공 시 JwtTokenProvider가 토큰을 발급하는데, AuthService.issueToken() 전에
 *        assertNotSuspended()로 정지 계정 로그인을 막고 있다 — 이 순서를 바꾸면 정지 계정이
 *        출석 포인트를 먼저 받아버릴 수 있다.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ApiResponse<LoginResponse> signup(@Valid @RequestBody SignupRequest req) {
        return ApiResponse.ok(authService.signup(req));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return ApiResponse.ok(authService.login(req));
    }

    @PostMapping("/social/{provider}")
    public ApiResponse<LoginResponse> socialLogin(@PathVariable String provider,
            @RequestBody Map<String, String> body) {
        return ApiResponse.ok(
                authService.socialLogin(provider, body.get("providerUserId"), body.get("email"), body.get("nickname")));
    }

    @PostMapping("/kakao/login")
    public ApiResponse<LoginResponse> kakaoLogin(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(authService.kakaoLogin(body.get("code")));
    }

    @PostMapping("/google/login")
    public ApiResponse<LoginResponse> googleLogin(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(authService.googleLogin(body.get("code")));
    }

}
