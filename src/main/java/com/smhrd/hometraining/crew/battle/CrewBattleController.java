package com.smhrd.hometraining.crew.battle;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smhrd.hometraining.common.ApiResponse;
import com.smhrd.hometraining.crew.battle.dto.CrewBattleDto;
import com.smhrd.hometraining.crew.battle.dto.CrewBattleParticipantResponse;
import com.smhrd.hometraining.crew.battle.dto.CrewBattleResultResponse;
import com.smhrd.hometraining.security.CustomUserPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [담당] 크루대전 매칭 신청/취소/조회(REST) — 실시간 렙 판정 반영은 CrewBattleSocketController(STOMP).
 * [프론트 연동] ounhome-f/js/crew.js의 크루대전 파트(requestCrewBattle 등) → /api/crew-battles/**.
 * [DB] CrewBattleService → CrewBattleRepository/CrewBattleParticipantRepository →
 *      crew_battles, crew_battle_participants 테이블.
 * [주의] 매칭 로직(대기 중인 상대 찾기)은 동시 요청 경합이 있는 구간이라 CrewBattleService에서
 *        비관적 락(findByIdForUpdate 등)을 쓴다 — 락 순서를 바꾸면 데드락 위험이 있다.
 */
@RestController
@RequestMapping("/api/crew-battles")
@RequiredArgsConstructor
public class CrewBattleController {

    private final CrewBattleService crewBattleService;

    /**
     * 크루대전 자동 매칭을 신청합니다.
     *
     * 크루장뿐 아니라 모든 크루원이 신청할 수 있습니다.
     *
     * 신청자는 참가자 목록에 반드시 포함되어야 하며,
     * 같은 팀 크기와 운동 종류로 대기 중인 크루가 있으면
     * 즉시 매칭됩니다.
     *
     * 상대가 없으면 WAITING 상태로 반환됩니다.
     */
    @PostMapping
    public ApiResponse<CrewBattleDto.Response> requestMatching(
            @AuthenticationPrincipal
            CustomUserPrincipal principal,

            @Valid
            @RequestBody
            CrewBattleDto.CreateRequest request
    ) {

        CrewBattleDto.Response response =
                crewBattleService.request(
                        principal.getUserId(),
                        request
                );

        return ApiResponse.ok(response);
    }

    /**
     * 본인이 신청한 자동 매칭 대기를 취소합니다.
     *
     * WAITING 상태에서만 취소할 수 있고,
     * 최초 신청자만 취소할 수 있습니다.
     */
    @DeleteMapping("/{battleId}/matching")
    public ApiResponse<CrewBattleDto.Response> cancelMatching(
            @AuthenticationPrincipal
            CustomUserPrincipal principal,

            @PathVariable
            Long battleId
    ) {

        CrewBattleDto.Response response =
                crewBattleService.cancelMatching(
                        principal.getUserId(),
                        battleId
                );

        return ApiResponse.ok(response);
    }

    /**
     * 크루대전 한 건을 조회합니다.
     *
     * 대전 시간이 끝났다면 조회 시점에
     * 자동으로 종료 및 경험치 지급을 처리합니다.
     */
    @GetMapping("/{battleId}")
    public ApiResponse<CrewBattleDto.Response> get(
            @AuthenticationPrincipal
            CustomUserPrincipal principal,

            @PathVariable
            Long battleId
    ) {

        CrewBattleDto.Response response =
                crewBattleService.get(
                        principal.getUserId(),
                        battleId
                );

        return ApiResponse.ok(response);
    }

    /**
     * 현재 사용자가 속한 크루의
     * 전체 대전 내역을 최신순으로 조회합니다.
     */
    @GetMapping("/me")
    public ApiResponse<List<CrewBattleDto.Response>> mine(
            @AuthenticationPrincipal
            CustomUserPrincipal principal
    ) {

        List<CrewBattleDto.Response> response =
                crewBattleService.getMine(
                        principal.getUserId()
                );

        return ApiResponse.ok(response);
    }

    /**
     * 종료된 크루대전의 상세 결과를 조회합니다.
     *
     * 양쪽 크루의 최종 운동 횟수, 점수,
     * 승패 결과, 지급 경험치와 참가자별 기록을 반환합니다.
     */
    @GetMapping("/{battleId}/result")
    public ApiResponse<CrewBattleResultResponse> result(
            @AuthenticationPrincipal
            CustomUserPrincipal principal,

            @PathVariable
            Long battleId
    ) {

        CrewBattleResultResponse response =
                crewBattleService.getResult(
                        principal.getUserId(),
                        battleId
                );

        return ApiResponse.ok(response);
    }

    /**
     * 매칭된 대전의 참가자 목록과
     * 참가자별 현재 운동 기록을 조회합니다.
     */
    @GetMapping("/{battleId}/participants")
    public ApiResponse<List<CrewBattleParticipantResponse>> participants(
            @AuthenticationPrincipal
            CustomUserPrincipal principal,

            @PathVariable
            Long battleId
    ) {

        List<CrewBattleParticipantResponse> response =
                crewBattleService.getParticipants(
                        principal.getUserId(),
                        battleId
                );

        return ApiResponse.ok(response);
    }
}