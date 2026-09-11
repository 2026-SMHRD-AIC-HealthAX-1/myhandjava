package com.smhrd.hometraining.ranking;

import com.smhrd.hometraining.common.ApiResponse;
import com.smhrd.hometraining.ranking.dto.CrewRankingRowResponse;
import com.smhrd.hometraining.ranking.dto.RankingRowResponse;
import com.smhrd.hometraining.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rankings")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    @GetMapping("/region")
    public ApiResponse<List<RankingRowResponse>> region(@RequestParam String city, @RequestParam String gu,
                                                          @RequestParam String dong,
                                                          @AuthenticationPrincipal CustomUserPrincipal principal) {
        return ApiResponse.ok(rankingService.regionRanking(city, gu, dong, principal == null ? null : principal.getUserId()));
    }

    @GetMapping("/exercise")
    public ApiResponse<List<RankingRowResponse>> exercise(@RequestParam String city, @RequestParam String gu,
                                                            @RequestParam String dong, @RequestParam String exerciseType,
                                                            @AuthenticationPrincipal CustomUserPrincipal principal) {
        return ApiResponse.ok(rankingService.exerciseRanking(city, gu, dong, exerciseType,
                principal == null ? null : principal.getUserId()));
    }

    @GetMapping("/crew")
    public ApiResponse<List<CrewRankingRowResponse>> crew(@RequestParam String city, @RequestParam String gu) {
        return ApiResponse.ok(rankingService.crewRanking(city, gu));
    }
}
