package com.smhrd.hometraining.admin;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smhrd.hometraining.admin.dto.AdminDashboardResponse;
import com.smhrd.hometraining.common.ApiResponse;

import lombok.RequiredArgsConstructor;

/** 관리자 대시보드 화면 전용 API. */
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
