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
