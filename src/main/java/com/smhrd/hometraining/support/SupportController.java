package com.smhrd.hometraining.support;

import com.smhrd.hometraining.common.ApiResponse;
import com.smhrd.hometraining.security.CustomUserPrincipal;
import com.smhrd.hometraining.support.dto.SupportTicketDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * [담당] 고객센터 문의 등록/내 문의 조회(일반) + 전체 조회/답변/처리중 표시(관리자, @PreAuthorize).
 * [프론트 연동] ounhome-f/js/support.js가 전부 담당 — admin.js도 이 파일의 render/load 함수를
 *              그대로 재사용해서 관리자모드 '고객센터 문의 관리' 탭을 그린다.
 * [DB] SupportService → SupportTicketRepository → support_tickets 테이블.
 */
@RestController
@RequestMapping("/api/support/tickets")
@RequiredArgsConstructor
public class SupportController {

    private final SupportService supportService;

    @PostMapping
    public ApiResponse<SupportTicketDto> create(@AuthenticationPrincipal CustomUserPrincipal principal,
                                                 @Valid @RequestBody SupportTicketDto.Create req) {
        return ApiResponse.ok(supportService.create(principal.getUserId(), req));
    }

    @GetMapping("/me")
    public ApiResponse<List<SupportTicketDto>> myTickets(@AuthenticationPrincipal CustomUserPrincipal principal) {
        return ApiResponse.ok(supportService.myTickets(principal.getUserId()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<SupportTicketDto>> all() {
        return ApiResponse.ok(supportService.allTickets());
    }

    @PatchMapping("/{ticketId}/reply")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SupportTicketDto> reply(@PathVariable Long ticketId, @Valid @RequestBody SupportTicketDto.Reply req) {
        return ApiResponse.ok(supportService.reply(ticketId, req));
    }

    @PatchMapping("/{ticketId}/start")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SupportTicketDto> start(@PathVariable Long ticketId) {
        return ApiResponse.ok(supportService.markInProgress(ticketId));
    }
}
