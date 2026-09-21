package com.smhrd.hometraining.crew;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smhrd.hometraining.common.ApiResponse;
import com.smhrd.hometraining.crew.dto.CrewBattleContributionResponse;
import com.smhrd.hometraining.crew.dto.CrewChatMessageDto;
import com.smhrd.hometraining.crew.dto.CrewCreateRequest;
import com.smhrd.hometraining.crew.dto.CrewExperienceHistoryResponse;
import com.smhrd.hometraining.crew.dto.CrewJoinRequestDto;
import com.smhrd.hometraining.crew.dto.CrewJoinSettingRequest;
import com.smhrd.hometraining.crew.dto.CrewNoticeDto;
import com.smhrd.hometraining.crew.dto.CrewResponse;
import com.smhrd.hometraining.crew.dto.CrewSummaryResponse;
import com.smhrd.hometraining.crew.dto.CrewUpdateRequest;
import com.smhrd.hometraining.crew.dto.CrewWeeklyMissionResponse;
import com.smhrd.hometraining.security.CustomUserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [담당] '홈크루' 카테고리 REST API 대부분 — 생성/조회/가입신청/승인/탈퇴/해체/공지/채팅기록/
 *        주간미션/경험치이력/대전기여도. (실시간 채팅 발신·크루대전 자체는 별도 컨트롤러 참고)
 * [프론트 연동] ounhome-f/js/crew.js가 이 컨트롤러의 거의 모든 엔드포인트를 호출한다(fetch로 직접).
 * [DB] 대부분 CrewService 한 곳을 거쳐 crews/crew_members/crew_join_requests/crew_notices 등
 *      크루 관련 테이블에 접근한다 — 다른 크루 관련 컨트롤러(AdminCrewChatReportController,
 *      CrewChatController, CrewBattlePartyController)도 같은 CrewService를 공유한다.
 * [주의] CrewService가 이 프로젝트에서 가장 큰 "갓 서비스"라 메서드가 아주 많다 — 여기서 새
 *        엔드포인트를 추가할 땐 비슷한 기존 메서드가 있는지 먼저 찾아볼 것.
 */
@RestController
@RequestMapping("/api/crews")
@RequiredArgsConstructor
public class CrewController {

    private final CrewService crewService;

    private final CrewWeeklyMissionService
            crewWeeklyMissionService;

    private final CrewExperienceService
            crewExperienceService;

    private final CrewBattleContributionService
            crewBattleContributionService;

    /**
     * 전체 또는 지역별 크루 목록을 조회합니다.
     *
     * 비로그인 사용자도 조회할 수 있습니다.
     */
    @GetMapping("/browse")
    public ApiResponse<List<CrewSummaryResponse>> browse(
            @RequestParam(required = false)
            String regionCity,

            @RequestParam(required = false)
            String regionGu,

            @AuthenticationPrincipal
            CustomUserPrincipal principal
    ) {

        Long viewerUserId =
                principal != null
                        ? principal.getUserId()
                        : null;

        List<CrewSummaryResponse> response =
                crewService.browse(
                        regionCity,
                        regionGu,
                        viewerUserId
                );

        return ApiResponse.ok(response);
    }

    /**
     * 현재 사용자가 소속된 크루를 조회합니다.
     */
    @GetMapping("/me")
    public ApiResponse<CrewResponse> myCrew(
            @AuthenticationPrincipal
            CustomUserPrincipal principal
    ) {

        CrewResponse response =
                crewService.getMyCrew(
                                principal.getUserId()
                        )
                        .orElse(null);

        return ApiResponse.ok(response);
    }

    /**
     * 현재 사용자가 소속된 크루의
     * 이번 주 주간 미션 현황을 조회합니다.
     */
    @GetMapping("/me/weekly-mission")
    public ApiResponse<CrewWeeklyMissionResponse> getWeeklyMission(
            @AuthenticationPrincipal
            CustomUserPrincipal principal
    ) {

        CrewWeeklyMissionResponse response =
                crewWeeklyMissionService
                        .getCurrentMission(
                                principal.getUserId()
                        );

        return ApiResponse.ok(response);
    }

    /**
     * 현재 사용자가 소속된 크루의
     * 사용자별 크루대전 누적 기여도를 조회합니다.
     */
    @GetMapping("/me/battle-contributions")
    public ApiResponse<List<CrewBattleContributionResponse>>
            battleContributions(
                    @AuthenticationPrincipal
                    CustomUserPrincipal principal
            ) {

        List<CrewBattleContributionResponse> response =
                crewBattleContributionService
                        .getMyCrewContributions(
                                principal.getUserId()
                        );

        return ApiResponse.ok(response);
    }

    /**
     * 현재 사용자가 소속된 크루의
     * 경험치 지급 내역을 최신순으로 조회합니다.
     */
    @GetMapping("/me/experience-history")
    public ApiResponse<List<CrewExperienceHistoryResponse>>
            experienceHistory(
                    @AuthenticationPrincipal
                    CustomUserPrincipal principal
            ) {

        List<CrewExperienceHistoryResponse> response =
                crewExperienceService
                        .getMyCrewHistory(
                                principal.getUserId()
                        );

        return ApiResponse.ok(response);
    }

    /**
     * 새로운 크루를 생성합니다.
     */
    @PostMapping
    public ApiResponse<CrewResponse> create(
            @AuthenticationPrincipal
            CustomUserPrincipal principal,

            @Valid
            @RequestBody
            CrewCreateRequest request
    ) {

        CrewResponse response =
                crewService.create(
                        principal.getUserId(),
                        request
                );

        return ApiResponse.ok(response);
    }

    /**
     * 크루 설명과 콘셉트를 수정합니다.
     *
     * 크루장만 사용할 수 있습니다.
     */
    @PatchMapping("/me")
    public ApiResponse<CrewResponse> update(
            @AuthenticationPrincipal
            CustomUserPrincipal principal,

            @Valid
            @RequestBody
            CrewUpdateRequest request
    ) {

        CrewResponse response =
                crewService.updateCrew(
                        principal.getUserId(),
                        request
                );

        return ApiResponse.ok(response);
    }

    /**
     * 크루 가입 신청 자동승인 여부를 변경합니다.
     *
     * 크루장만 사용할 수 있습니다.
     */
    @PatchMapping("/me/join-setting")
    public ApiResponse<CrewResponse> updateJoinSetting(
            @AuthenticationPrincipal
            CustomUserPrincipal principal,

            @Valid
            @RequestBody
            CrewJoinSettingRequest request
    ) {

        CrewResponse response =
                crewService.updateAutoApprove(
                        principal.getUserId(),
                        request.autoApprove()
                );

        return ApiResponse.ok(response);
    }

    /**
     * 크루 가입을 신청합니다.
     */
    @PostMapping("/{crewId}/join-requests")
    public ApiResponse<Void> requestJoin(
            @AuthenticationPrincipal
            CustomUserPrincipal principal,

            @PathVariable
            Long crewId,

            @RequestBody
            CrewJoinRequestDto.Create request
    ) {

        crewService.requestJoin(
                principal.getUserId(),
                crewId,
                request.message()
        );

        return ApiResponse.ok();
    }

    /**
     * 크루장이 가입 신청 목록을 조회합니다.
     */
    @GetMapping("/me/join-requests")
    public ApiResponse<List<CrewJoinRequestDto>> joinRequests(
            @AuthenticationPrincipal
            CustomUserPrincipal principal
    ) {

        List<CrewJoinRequestDto> response =
                crewService.listJoinRequests(
                        principal.getUserId()
                );

        return ApiResponse.ok(response);
    }

    /**
     * 크루 가입 신청을 승인합니다.
     */
    @PostMapping("/me/join-requests/{requestId}/approve")
    public ApiResponse<Void> approve(
            @AuthenticationPrincipal
            CustomUserPrincipal principal,

            @PathVariable
            Long requestId
    ) {

        crewService.approveJoinRequest(
                principal.getUserId(),
                requestId
        );

        return ApiResponse.ok();
    }

    /**
     * 크루 가입 신청을 거절합니다.
     */
    @PostMapping("/me/join-requests/{requestId}/reject")
    public ApiResponse<Void> reject(
            @AuthenticationPrincipal
            CustomUserPrincipal principal,

            @PathVariable
            Long requestId
    ) {

        crewService.rejectJoinRequest(
                principal.getUserId(),
                requestId
        );

        return ApiResponse.ok();
    }

    /**
     * 현재 크루장이 다른 크루원에게
     * 크루장 권한을 양도합니다.
     */
    @PatchMapping("/me/leader/{targetUserId}")
    public ApiResponse<CrewResponse> transferLeadership(
            @AuthenticationPrincipal
            CustomUserPrincipal principal,

            @PathVariable
            Long targetUserId
    ) {

        CrewResponse response =
                crewService.transferLeadership(
                        principal.getUserId(),
                        targetUserId
                );

        return ApiResponse.ok(response);
    }

    /**
     * 크루원을 강퇴합니다.
     *
     * 크루장만 사용할 수 있습니다.
     */
    @DeleteMapping("/me/members/{targetUserId}")
    public ApiResponse<Void> kick(
            @AuthenticationPrincipal
            CustomUserPrincipal principal,

            @PathVariable
            Long targetUserId
    ) {

        crewService.kickMember(
                principal.getUserId(),
                targetUserId
        );

        return ApiResponse.ok();
    }

    /**
     * 현재 크루에서 탈퇴합니다.
     *
     * 크루장 혼자 남아 있다면
     * 크루도 함께 삭제됩니다.
     */
    @PostMapping("/me/leave")
    public ApiResponse<Void> leave(
            @AuthenticationPrincipal
            CustomUserPrincipal principal
    ) {

        crewService.leave(
                principal.getUserId()
        );

        return ApiResponse.ok();
    }

    /**
     * 크루장이 크루를 직접 해체합니다.
     *
     * 크루장 혼자 남아 있는 경우에만 가능합니다.
     */
    @DeleteMapping("/me")
    public ApiResponse<Void> disband(
            @AuthenticationPrincipal
            CustomUserPrincipal principal
    ) {

        crewService.disband(
                principal.getUserId()
        );

        return ApiResponse.ok();
    }

    /**
     * 현재 크루의 공지사항을 조회합니다.
     */
    @GetMapping("/me/notices")
    public ApiResponse<List<CrewNoticeDto>> notices(
            @AuthenticationPrincipal
            CustomUserPrincipal principal
    ) {

        List<CrewNoticeDto> response =
                crewService.listNotices(
                        principal.getUserId()
                );

        return ApiResponse.ok(response);
    }

    /**
     * 크루 공지사항을 등록합니다.
     *
     * 크루장만 사용할 수 있습니다.
     */
    @PostMapping("/me/notices")
    public ApiResponse<CrewNoticeDto> addNotice(
            @AuthenticationPrincipal
            CustomUserPrincipal principal,

            @Valid
            @RequestBody
            CrewNoticeDto.Create request
    ) {

        CrewNoticeDto response =
                crewService.addNotice(
                        principal.getUserId(),
                        request
                );

        return ApiResponse.ok(response);
    }

    /**
     * 최근 크루 채팅 50개를 조회합니다.
     */
    @GetMapping("/me/chat")
    public ApiResponse<List<CrewChatMessageDto>> recentChat(
            @AuthenticationPrincipal
            CustomUserPrincipal principal
    ) {

        List<CrewChatMessageDto> response =
                crewService.recentChat(
                        principal.getUserId()
                );

        return ApiResponse.ok(response);
    }

    /**
     * 크루채팅 메시지를 신고합니다.
     */
    @PostMapping("/me/chat/{messageId}/report")
    public ApiResponse<Void> reportChat(
            @AuthenticationPrincipal
            CustomUserPrincipal principal,

            @PathVariable
            Long messageId
    ) {

        crewService.reportChatMessage(
                principal.getUserId(),
                messageId
        );

        return ApiResponse.ok();
    }
}
