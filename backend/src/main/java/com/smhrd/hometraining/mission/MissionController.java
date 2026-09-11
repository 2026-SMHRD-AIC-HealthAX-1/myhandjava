package com.smhrd.hometraining.mission;

import com.smhrd.hometraining.common.ApiResponse;
import com.smhrd.hometraining.mission.dto.MissionResponse;
import com.smhrd.hometraining.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/missions")
@RequiredArgsConstructor
public class MissionController {

    private final MissionService missionService;

    @GetMapping("/today")
    public ApiResponse<List<MissionResponse>> today(@AuthenticationPrincipal CustomUserPrincipal principal) {
        return ApiResponse.ok(missionService.getTodayMissions(principal.getUserId()));
    }

    @PostMapping("/{missionId}/claim")
    public ApiResponse<Map<String, Long>> claim(@AuthenticationPrincipal CustomUserPrincipal principal,
                                                  @PathVariable Long missionId) {
        return ApiResponse.ok(Map.of("pointsAwarded", missionService.claim(principal.getUserId(), missionId)));
    }
}
