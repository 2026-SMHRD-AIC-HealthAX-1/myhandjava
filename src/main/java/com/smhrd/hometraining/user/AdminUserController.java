package com.smhrd.hometraining.user;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smhrd.hometraining.common.ApiResponse;
import com.smhrd.hometraining.user.dto.AdminUserResponse;

import lombok.RequiredArgsConstructor;

/**
 * 관리자 "전체 사용자 관리" 화면 전용 API — 전체 회원 조회/검색과 정지·활성 처리.
 *
 * [프론트 연동] ounhome-f/js/admin.js loadAdminUsers()/suspendAdminUser()/activateAdminUser().
 * [DB] UserService → UserRepository → users 테이블(status 컬럼을 ACTIVE/SUSPENDED로 갱신).
 * [주의] ⚠️ suspend() 이후 정지된 계정은 AuthService.assertNotSuspended()에서 다음 로그인
 *        시도부터 거부된다(이미 발급된 토큰은 만료 전까지 유효 — 즉시 강제 로그아웃은 아님).
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ApiResponse<List<AdminUserResponse>> list(
            @RequestParam(required = false) String search
    ) {
        return ApiResponse.ok(userService.listUsersForAdmin(search));
    }

    @PatchMapping("/{userId}/suspend")
    public ApiResponse<Void> suspend(@PathVariable Long userId) {
        userService.suspendUser(userId);
        return ApiResponse.ok();
    }

    @PatchMapping("/{userId}/activate")
    public ApiResponse<Void> activate(@PathVariable Long userId) {
        userService.activateUser(userId);
        return ApiResponse.ok();
    }
}
