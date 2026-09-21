package com.smhrd.hometraining.mission;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smhrd.hometraining.common.exception.BusinessException;
import com.smhrd.hometraining.mission.dto.MissionDefinitionRequest;
import com.smhrd.hometraining.mission.dto.MissionDefinitionResponse;
import com.smhrd.hometraining.mission.entity.MissionDefinition;
import com.smhrd.hometraining.mission.repository.MissionDefinitionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MissionDefinitionService {

    private final MissionDefinitionRepository
            missionDefinitionRepository;

    /**
     * 새로운 관리자 미션 원본을 등록합니다.
     */
    @Transactional
    public MissionDefinitionResponse create(
            MissionDefinitionRequest request
    ) {

        String exerciseType =
                normalizeExerciseType(
                        request.exerciseType()
                );

        /*
         * 목표값이 미션 종류의 최소·최대 범위를
         * 벗어났는지 검사합니다.
         */
        boolean duplicated =
                missionDefinitionRepository
                        .existsByScopeAndMetricAndExerciseTypeAndMinTargetAndMaxTarget(
                                request.scope(),
                                request.metric(),
                                exerciseType,
                                request.minTarget(),
                                request.maxTarget()
                        );

        if (duplicated) {
            throw new BusinessException(
                    "동일한 종류와 목표값의 미션이 이미 등록되어 있습니다."
            );
        }

        MissionDefinition definition =
                MissionDefinition.create(
                        request.scope(),
                        request.metric(),
                        exerciseType,
                        request.minTarget(),
                        request.maxTarget(),
                        request.label(),
                        request.rewardPoints(),
                        request.rewardExp()
                );

        MissionDefinition savedDefinition =
                missionDefinitionRepository.save(
                        definition
                );

        return MissionDefinitionResponse.from(
                savedDefinition
        );
    }

    /**
     * 관리자가 등록한 전체 미션을 조회합니다.
     *
     * 활성 및 비활성 미션을 모두 반환합니다.
     */
    @Transactional(readOnly = true)
    public List<MissionDefinitionResponse> getAll() {

        return missionDefinitionRepository
                .findAllByOrderByIdAsc()
                .stream()
                .map(MissionDefinitionResponse::from)
                .toList();
    }


    /**
     * 기존 관리자 미션 원본을 수정합니다.
     */
    @Transactional
    public MissionDefinitionResponse update(
            Long definitionId,
            MissionDefinitionRequest request
    ) {

        MissionDefinition definition =
                getDefinitionOrThrow(
                        definitionId
                );

        String exerciseType =
                normalizeExerciseType(
                        request.exerciseType()
                );

        /*
         * 수정 중인 자기 자신을 제외하고
         * 동일한 미션이 있는지 확인합니다.
         */
        boolean duplicated =
                missionDefinitionRepository
                        .existsByScopeAndMetricAndExerciseTypeAndMinTargetAndMaxTargetAndIdNot(
                                request.scope(),
                                request.metric(),
                                exerciseType,
                                request.minTarget(),
                                request.maxTarget(),
                                definitionId
                        );

        if (duplicated) {
            throw new BusinessException(
                    "동일한 종류와 목표값의 미션이 이미 등록되어 있습니다."
            );
        }

        definition.update(
                request.scope(),
                request.metric(),
                exerciseType,
                request.minTarget(),
                request.maxTarget(),
                request.label(),
                request.rewardPoints(),
                request.rewardExp()
        );

        return MissionDefinitionResponse.from(
                definition
        );
    }

    /**
     * 미션을 활성화합니다.
     */
    @Transactional
    public MissionDefinitionResponse activate(
            Long definitionId
    ) {

        MissionDefinition definition =
                getDefinitionOrThrow(
                        definitionId
                );

        definition.activate();

        return MissionDefinitionResponse.from(
                definition
        );
    }

    /**
     * 미션을 비활성화합니다.
     *
     * DB에서 삭제하지 않고 신규 배정에서만 제외합니다.
     */
    @Transactional
    public MissionDefinitionResponse deactivate(
            Long definitionId
    ) {

        MissionDefinition definition =
                getDefinitionOrThrow(
                        definitionId
                );

        definition.deactivate();

        return MissionDefinitionResponse.from(
                definition
        );
    }

    /**
     * ID로 관리자 미션 원본을 조회합니다.
     */
    private MissionDefinition getDefinitionOrThrow(
            Long definitionId
    ) {

        if (definitionId == null) {
            throw new BusinessException(
                    "미션 ID가 필요합니다."
            );
        }

        return missionDefinitionRepository
                .findById(definitionId)
                .orElseThrow(() ->
                        new BusinessException(
                                "관리자 미션을 찾을 수 없습니다."
                        )
                );
    }

    /**
     * 운동 종류의 앞뒤 공백을 제거하고
     * 대문자로 통일합니다.
     */
    private String normalizeExerciseType(
            String exerciseType
    ) {

        if (exerciseType == null
                || exerciseType.isBlank()) {

            throw new BusinessException(
                    "운동 종류가 필요합니다."
            );
        }

        return exerciseType
                .trim()
                .toUpperCase(Locale.ROOT);
    }
}
