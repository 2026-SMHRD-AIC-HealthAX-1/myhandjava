package com.smhrd.hometraining.admin.dto;

import java.util.List;

import com.smhrd.hometraining.crew.dto.CrewChatReportResponse;
import com.smhrd.hometraining.support.dto.SupportTicketDto;

/** 관리자 대시보드 화면 한 번에 필요한 통계 + 최근 항목들을 묶은 응답. */
public record AdminDashboardResponse(
        long totalUsers,
        long todayExerciseCount,
        long pendingReportCount,
        long unansweredTicketCount,
        List<CrewChatReportResponse> recentReports,
        List<SupportTicketDto> recentTickets
) {
}
