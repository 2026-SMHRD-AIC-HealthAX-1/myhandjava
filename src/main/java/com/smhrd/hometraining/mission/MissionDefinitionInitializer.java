package com.smhrd.hometraining.mission;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.smhrd.hometraining.mission.entity.MissionDefinition;
import com.smhrd.hometraining.mission.entity.MissionMetric;
import com.smhrd.hometraining.mission.entity.MissionScope;
import com.smhrd.hometraining.mission.repository.MissionDefinitionRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MissionDefinitionInitializer
        implements ApplicationRunner {

    private static final String DEFAULT_EXERCISE_TYPE =
            "SQUAT";

    private final MissionDefinitionRepository
            missionDefinitionRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        createIfMissing(
                MissionScope.PERSONAL,
                MissionMetric.REPS,
                20,
                20,
                "스쿼트 20회 달성",
                MissionDefinition.DEFAULT_REWARD_POINTS,
                MissionDefinition.DEFAULT_REWARD_EXP
        );

        createIfMissing(
                MissionScope.PERSONAL,
                MissionMetric.PERFECT,
                5,
                5,
                "스쿼트 PERFECT 5회 만들기",
                MissionDefinition.DEFAULT_REWARD_POINTS,
                MissionDefinition.DEFAULT_REWARD_EXP
        );

        createIfMissing(
                MissionScope.PERSONAL,
                MissionMetric.SESSIONS,
                2,
                2,
                "스쿼트 세트 2회 완료",
                MissionDefinition.DEFAULT_REWARD_POINTS,
                MissionDefinition.DEFAULT_REWARD_EXP
        );

        createIfMissing(
                MissionScope.PERSONAL,
                MissionMetric.MISS_FREE_SESSION,
                1,
                1,
                "MISS 없이 스쿼트 세트 1회 완료",
                MissionDefinition.DEFAULT_REWARD_POINTS,
                MissionDefinition.DEFAULT_REWARD_EXP
        );

        createIfMissing(
                MissionScope.PERSONAL,
                MissionMetric.ACC_SESSION,
                1,
                1,
                "정확도 90% 이상 세트 1회 달성",
                MissionDefinition.DEFAULT_REWARD_POINTS,
                MissionDefinition.DEFAULT_REWARD_EXP
        );

        createIfMissing(
                MissionScope.CREW,
                MissionMetric.REPS,
                300,
                300,
                "크루 주간 운동 300회 달성",
                0,
                500
        );
    }

    private void createIfMissing(
            MissionScope scope,
            MissionMetric metric,
            int minTarget,
            int maxTarget,
            String label,
            int rewardPoints,
            int rewardExp
    ) {

        boolean exists =
                missionDefinitionRepository
                        .existsByScopeAndMetricAndExerciseTypeAndMinTargetAndMaxTarget(
                                scope,
                                metric,
                                DEFAULT_EXERCISE_TYPE,
                                minTarget,
                                maxTarget
                        );

        if (exists) {
            return;
        }

        missionDefinitionRepository.save(
                MissionDefinition.create(
                        scope,
                        metric,
                        DEFAULT_EXERCISE_TYPE,
                        minTarget,
                        maxTarget,
                        label,
                        rewardPoints,
                        rewardExp
                )
        );
    }
}
