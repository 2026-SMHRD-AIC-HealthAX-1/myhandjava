package com.smhrd.hometraining.crew.dto;

import java.time.LocalDateTime;

import com.smhrd.hometraining.crew.entity.CrewChatReport;

/** 관리자 "크루채팅 신고 관리" 화면에서 쓰는 신고 한 건 요약. */
public record CrewChatReportResponse(
        Long id,
        String reporterNickname,
        Long targetUserId,
        String targetNickname,
        String messageText,
        LocalDateTime chatSentAt,
        LocalDateTime reportedAt,
        String status
) {

    public static CrewChatReportResponse from(CrewChatReport report) {
        return new CrewChatReportResponse(
                report.getId(),
                report.getReporter().getNickname(),
                report.getTargetUser().getId(),
                report.getTargetUser().getNickname(),
                report.getMessageTextSnapshot(),
                report.getMessage().getSentAt(),
                report.getReportedAt(),
                report.getStatus().name()
        );
    }
}
