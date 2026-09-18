package com.smhrd.hometraining.mission;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smhrd.hometraining.common.ApiResponse;
import com.smhrd.hometraining.mission.dto.MissionClaimResponse;
import com.smhrd.hometraining.mission.dto.MissionResponse;
import com.smhrd.hometraining.security.CustomUserPrincipal;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/missions")
@RequiredArgsConstructor
public class MissionController {

    private final MissionService missionService;

    /**
     * 로그인한 사용자의 오늘 일일 미션 3개를 조회합니다.
     *
     * 오늘 미션이 아직 없다면 활성 관리자 미션 중
     * 3개를 무작위로 배정합니다.
     */
    @GetMapping("/today")
    public ApiResponse<List<MissionResponse>> getTodayMissions(
            @AuthenticationPrincipal
            CustomUserPrincipal principal
    ) {

        List<MissionResponse> response =
                missionService.getTodayMissions(
                        principal.getUserId()
                );

        return ApiResponse.ok(response);
    }

    /**
     * 완료한 일일 미션의 포인트와 경험치 보상을 받습니다.
     */
    @PostMapping("/{missionId}/claim")
    public ApiResponse<MissionClaimResponse> claim(
            @AuthenticationPrincipal
            CustomUserPrincipal principal,

            @PathVariable
            Long missionId
    ) {

        MissionClaimResponse response =
                missionService.claim(
                        principal.getUserId(),
                        missionId
                );

        return ApiResponse.ok(response);
    }
}