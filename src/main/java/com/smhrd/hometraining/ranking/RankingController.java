package com.smhrd.hometraining.ranking;

import com.smhrd.hometraining.common.ApiResponse;
import com.smhrd.hometraining.ranking.dto.CrewRankingRowResponse;
import com.smhrd.hometraining.ranking.dto.RankingRowResponse;
import com.smhrd.hometraining.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * [담당] 지역별/종목별/크루 랭킹 산출 — 별도 랭킹 테이블 없이 매 요청마다 집계 쿼리로 계산한다.
 * [프론트 연동] ounhome-f/js/ranking.js — GET /api/rankings/region, /exercise, /crew.
 * [DB] RankingService → UserRepository/CrewRepository의 JPQL 집계 쿼리(findRegionRanking 등)
 *      → users, exercise_records, crews 조인. city/gu/dong이 null이면 그 단위는 필터링 안 함
 *      ("전체" 선택 시 전국 랭킹).
 */
@RestController
@RequestMapping("/api/rankings")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    @GetMapping("/region")
    public ApiResponse<List<RankingRowResponse>> region(@RequestParam(required = false) String city,
                                                          @RequestParam(required = false) String gu,
                                                          @RequestParam(required = false) String dong,
                                                          @AuthenticationPrincipal CustomUserPrincipal principal) {
        return ApiResponse.ok(rankingService.regionRanking(city, gu, dong, principal == null ? null : principal.getUserId()));
    }

    @GetMapping("/exercise")
    public ApiResponse<List<RankingRowResponse>> exercise(@RequestParam(required = false) String city,
                                                            @RequestParam(required = false) String gu,
                                                            @RequestParam(required = false) String dong,
                                                            @RequestParam String exerciseType,
                                                            @AuthenticationPrincipal CustomUserPrincipal principal) {
        return ApiResponse.ok(rankingService.exerciseRanking(city, gu, dong, exerciseType,
                principal == null ? null : principal.getUserId()));
    }

    @GetMapping("/crew")
    public ApiResponse<List<CrewRankingRowResponse>> crew(@RequestParam(required = false) String city,
                                                            @RequestParam(required = false) String gu) {
        return ApiResponse.ok(rankingService.crewRanking(city, gu));
    }
}
