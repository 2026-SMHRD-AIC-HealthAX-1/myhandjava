package com.smhrd.hometraining.auth;

import com.smhrd.hometraining.auth.dto.*;
import com.smhrd.hometraining.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ApiResponse<LoginResponse> signup(@Valid @RequestBody SignupRequest req) {
        return ApiResponse.ok(authService.signup(req));
    }

    @PostMapping("/check-id")
    public ApiResponse<Map<String, Boolean>> checkId(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(Map.of("duplicate", authService.isLoginIdTaken(body.get("loginId"))));
    }

    @PostMapping("/check-nickname")
    public ApiResponse<Map<String, Boolean>> checkNickname(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(Map.of("duplicate", authService.isNicknameTaken(body.get("nickname"))));
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

    @PostMapping("/find-id")
    public ApiResponse<Map<String, String>> findId(@Valid @RequestBody FindIdRequest req) {
        return ApiResponse.ok(Map.of("loginId", authService.findLoginIdByEmail(req)));
    }

    /** 실제 서비스에서는 임시 비밀번호를 이메일로만 발송하고 응답에는 담지 않아야 한다. */
    @PostMapping("/find-password")
    public ApiResponse<Map<String, String>> findPassword(@Valid @RequestBody FindPasswordRequest req) {
        return ApiResponse.ok(Map.of("temporaryPassword", authService.issueTemporaryPassword(req)));
    }

    @PostMapping("/check-email")
    public ApiResponse<Map<String, Boolean>> checkEmail(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(Map.of("duplicate", authService.isEmailTaken(body.get("email"))));
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
