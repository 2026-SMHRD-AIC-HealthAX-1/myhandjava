package com.smhrd.hometraining.crew;

import com.smhrd.hometraining.common.ApiResponse;
import com.smhrd.hometraining.crew.dto.*;
import com.smhrd.hometraining.security.CustomUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/crews")
@RequiredArgsConstructor
public class CrewController {

    private final CrewService crewService;

    @GetMapping("/browse")
    public ApiResponse<List<CrewSummaryResponse>> browse(@RequestParam(required = false) String regionCity,
                                                           @RequestParam(required = false) String regionGu,
                                                           @AuthenticationPrincipal CustomUserPrincipal principal) {
        Long viewerUserId = principal != null ? principal.getUserId() : null;
        return ApiResponse.ok(crewService.browse(regionCity, regionGu, viewerUserId));
    }

    @GetMapping("/me")
    public ApiResponse<CrewResponse> myCrew(@AuthenticationPrincipal CustomUserPrincipal principal) {
        return ApiResponse.ok(crewService.getMyCrew(principal.getUserId()).orElse(null));
    }

    @PostMapping
    public ApiResponse<CrewResponse> create(@AuthenticationPrincipal CustomUserPrincipal principal,
                                             @Valid @RequestBody CrewCreateRequest req) {
        return ApiResponse.ok(crewService.create(principal.getUserId(), req));
    }

    @PatchMapping("/me")
    public ApiResponse<CrewResponse> update(@AuthenticationPrincipal CustomUserPrincipal principal,
                                             @RequestBody CrewUpdateRequest req) {
        return ApiResponse.ok(crewService.updateCrew(principal.getUserId(), req));
    }

    @PostMapping("/{crewId}/join-requests")
    public ApiResponse<Void> requestJoin(@AuthenticationPrincipal CustomUserPrincipal principal,
                                          @PathVariable Long crewId,
                                          @RequestBody CrewJoinRequestDto.Create req) {
        crewService.requestJoin(principal.getUserId(), crewId, req.message());
        return ApiResponse.ok();
    }

    @GetMapping("/me/join-requests")
    public ApiResponse<List<CrewJoinRequestDto>> joinRequests(@AuthenticationPrincipal CustomUserPrincipal principal) {
        return ApiResponse.ok(crewService.listJoinRequests(principal.getUserId()));
    }

    @PostMapping("/me/join-requests/{requestId}/approve")
    public ApiResponse<Void> approve(@AuthenticationPrincipal CustomUserPrincipal principal, @PathVariable Long requestId) {
        crewService.approveJoinRequest(principal.getUserId(), requestId);
        return ApiResponse.ok();
    }

    @PostMapping("/me/join-requests/{requestId}/reject")
    public ApiResponse<Void> reject(@AuthenticationPrincipal CustomUserPrincipal principal, @PathVariable Long requestId) {
        crewService.rejectJoinRequest(principal.getUserId(), requestId);
        return ApiResponse.ok();
    }

    @DeleteMapping("/me/members/{targetUserId}")
    public ApiResponse<Void> kick(@AuthenticationPrincipal CustomUserPrincipal principal, @PathVariable Long targetUserId) {
        crewService.kickMember(principal.getUserId(), targetUserId);
        return ApiResponse.ok();
    }

    @PostMapping("/me/leave")
    public ApiResponse<Void> leave(@AuthenticationPrincipal CustomUserPrincipal principal) {
        crewService.leave(principal.getUserId());
        return ApiResponse.ok();
    }

    @GetMapping("/me/notices")
    public ApiResponse<List<CrewNoticeDto>> notices(@AuthenticationPrincipal CustomUserPrincipal principal) {
        return ApiResponse.ok(crewService.listNotices(principal.getUserId()));
    }

    @PostMapping("/me/notices")
    public ApiResponse<CrewNoticeDto> addNotice(@AuthenticationPrincipal CustomUserPrincipal principal,
                                                 @Valid @RequestBody CrewNoticeDto.Create req) {
        return ApiResponse.ok(crewService.addNotice(principal.getUserId(), req));
    }

    @GetMapping("/me/chat")
    public ApiResponse<List<CrewChatMessageDto>> recentChat(@AuthenticationPrincipal CustomUserPrincipal principal) {
        return ApiResponse.ok(crewService.recentChat(principal.getUserId()));
    }
}
