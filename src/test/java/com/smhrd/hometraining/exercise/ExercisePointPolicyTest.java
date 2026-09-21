package com.smhrd.hometraining.exercise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.smhrd.hometraining.common.exception.BusinessException;
import com.smhrd.hometraining.exercise.policy.ExercisePointPolicy;

class ExercisePointPolicyTest {

    @Test
    void pointsAre40PercentOfScore() {

        assertEquals(
                0,
                ExercisePointPolicy.calculatePoints(0)
        );

        assertEquals(
                100,
                ExercisePointPolicy.calculatePoints(250)
        );

        assertEquals(
                200,
                ExercisePointPolicy.calculatePoints(500)
        );

        assertEquals(
                300,
                ExercisePointPolicy.calculatePoints(750)
        );

        assertEquals(
                388,
                ExercisePointPolicy.calculatePoints(970)
        );

        assertEquals(
                600,
                ExercisePointPolicy.calculatePoints(1500)
        );
    }

    @Test
    void negativeScoreThrowsException() {

        assertThrows(
                BusinessException.class,
                () -> ExercisePointPolicy.calculatePoints(-1)
        );
    }

    @Test
    void scoreAbove1500ThrowsException() {

        assertThrows(
                BusinessException.class,
                () -> ExercisePointPolicy.calculatePoints(1501)
        );
    }
}