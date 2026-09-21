package com.smhrd.hometraining.crew;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smhrd.hometraining.common.ApiResponse;
import com.smhrd.hometraining.crew.dto.CrewChatReportResponse;

import lombok.RequiredArgsConstructor;

/** 관리자 "크루채팅 신고 관리" 화면 전용 API. */
@RestController
@RequestMapping("/api/admin/crew-chat-reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCrewChatReportController {

    private final CrewService crewService;

    @GetMapping
    public ApiResponse<List<CrewChatReportResponse>> list() {
        return ApiResponse.ok(crewService.listChatReportsForAdmin());
    }

    @PatchMapping("/{reportId}/resolve")
    public ApiResponse<Void> resolve(@PathVariable Long reportId) {
        crewService.resolveChatReport(reportId);
        return ApiResponse.ok();
    }
}
