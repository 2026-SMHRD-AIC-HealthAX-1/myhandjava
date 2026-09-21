package com.smhrd.hometraining.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smhrd.hometraining.admin.dto.AdminDashboardResponse;
import com.smhrd.hometraining.common.ApiResponse;

import lombok.RequiredArgsConstructor;

/**
 * 관리자 대시보드 화면 전용 API.
 *
 * [담당] 관리자모드 대시보드(회원 수/오늘 운동 인증/미처리 신고/미답변 문의 요약).
 * [프론트 연동] ounhome-f/js/admin.js의 loadAdminDashboard() → GET /api/admin/dashboard.
 * [DB] 여기서 직접 쿼리하지 않고 AdminDashboardService가 여러 Repository를 모아 집계한다.
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping
    public ApiResponse<AdminDashboardResponse> get() {
        return ApiResponse.ok(adminDashboardService.getDashboard());
    }
}
