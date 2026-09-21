package com.smhrd.hometraining.mission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.smhrd.hometraining.common.exception.BusinessException;
import com.smhrd.hometraining.mission.entity.MissionDefinition;
import com.smhrd.hometraining.mission.entity.MissionMetric;
import com.smhrd.hometraining.mission.entity.MissionScope;

class MissionDefinitionTest {

    @Test
    void personalMissionStoresRangeAndRewards() {

        MissionDefinition definition =
                MissionDefinition.create(
                        MissionScope.PERSONAL,
                        MissionMetric.REPS,
                        "SQUAT",
                        15,
                        30,
                        "스쿼트 목표",
                        120,
                        80
                );

        assertEquals(15, definition.getMinTarget());
        assertEquals(30, definition.getMaxTarget());
        assertEquals(120, definition.getRewardPoints());
        assertEquals(80, definition.getRewardExp());
        assertTrue(definition.isActive());

        int target = definition.rollTarget(new Random(1));
        assertTrue(target >= 15 && target <= 30);

        definition.deactivate();
        assertFalse(definition.isActive());
    }

    @Test
    void crewMissionRejectsUnsupportedCondition() {

        assertThrows(
                BusinessException.class,
                () -> MissionDefinition.create(
                        MissionScope.CREW,
                        MissionMetric.SESSIONS,
                        "SQUAT",
                        1,
                        2,
                        "크루 세션 목표",
                        0,
                        500
                )
        );
    }
}
