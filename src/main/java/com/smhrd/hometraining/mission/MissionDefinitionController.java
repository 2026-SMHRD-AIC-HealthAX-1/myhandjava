package com.smhrd.hometraining.mission;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smhrd.hometraining.common.ApiResponse;
import com.smhrd.hometraining.mission.dto.MissionDefinitionRequest;
import com.smhrd.hometraining.mission.dto.MissionDefinitionResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [담당] 관리자용 미션 "원본"(정의) CRUD — 개인/크루 미션이 배정될 때 이 정의를 기준으로 뽑힌다.
 * [프론트 연동] ounhome-f/js/admin.js 관리자모드 '미션 관리' 탭(loadAdminMissions/submitAdminMission
 *              /toggleAdminMissionActive) → /api/admin/mission-definitions.
 * [DB] MissionDefinitionService → MissionDefinitionRepository → mission_definitions 테이블.
 */
@RestController
@RequestMapping("/api/admin/mission-definitions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class MissionDefinitionController {

    private final MissionDefinitionService
            missionDefinitionService;

    /**
     * 관리자 미션 원본을 새로 등록합니다.
     */
    @PostMapping
    public ApiResponse<MissionDefinitionResponse> create(
            @Valid
            @RequestBody
            MissionDefinitionRequest request
    ) {

        MissionDefinitionResponse response =
                missionDefinitionService.create(
                        request
                );

        return ApiResponse.ok(response);
    }

    /**
     * 활성 및 비활성 상태를 포함한
     * 전체 관리자 미션을 조회합니다.
     */
    @GetMapping
    public ApiResponse<List<MissionDefinitionResponse>> getAll() {

        List<MissionDefinitionResponse> response =
                missionDefinitionService.getAll();

        return ApiResponse.ok(response);
    }

    /**
     * 관리자 미션 원본을 수정합니다.
     */
    @PutMapping("/{definitionId}")
    public ApiResponse<MissionDefinitionResponse> update(
            @PathVariable
            Long definitionId,

            @Valid
            @RequestBody
            MissionDefinitionRequest request
    ) {

        MissionDefinitionResponse response =
                missionDefinitionService.update(
                        definitionId,
                        request
                );

        return ApiResponse.ok(response);
    }

    /**
     * 비활성화된 미션을 다시 활성화합니다.
     */
    @PatchMapping("/{definitionId}/activate")
    public ApiResponse<MissionDefinitionResponse> activate(
            @PathVariable
            Long definitionId
    ) {

        MissionDefinitionResponse response =
                missionDefinitionService.activate(
                        definitionId
                );

        return ApiResponse.ok(response);
    }

    /**
     * 미션을 삭제하지 않고 비활성화합니다.
     *
     * 비활성화된 미션은 사용자에게
     * 새롭게 배정되지 않습니다.
     */
    @PatchMapping("/{definitionId}/deactivate")
    public ApiResponse<MissionDefinitionResponse> deactivate(
            @PathVariable
            Long definitionId
    ) {

        MissionDefinitionResponse response =
                missionDefinitionService.deactivate(
                        definitionId
                );

        return ApiResponse.ok(response);
    }
}