package com.smhrd.hometraining.exercise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.smhrd.hometraining.common.exception.BusinessException;
import com.smhrd.hometraining.exercise.policy.ExerciseExpPolicy;

class ExerciseExpPolicyTest {

    @Test
    void scoreBelow250GivesZeroExp() {

        assertEquals(
                0,
                ExerciseExpPolicy.calculateExp(0)
        );

        assertEquals(
                0,
                ExerciseExpPolicy.calculateExp(249)
        );
    }

    @Test
    void scoreRangesGiveCorrectExp() {

        assertEquals(
                50,
                ExerciseExpPolicy.calculateExp(250)
        );

        assertEquals(
                50,
                ExerciseExpPolicy.calculateExp(499)
        );

        assertEquals(
                150,
                ExerciseExpPolicy.calculateExp(500)
        );

        assertEquals(
                150,
                ExerciseExpPolicy.calculateExp(749)
        );

        assertEquals(
                250,
                ExerciseExpPolicy.calculateExp(750)
        );

        assertEquals(
                250,
                ExerciseExpPolicy.calculateExp(899)
        );

        assertEquals(
                300,
                ExerciseExpPolicy.calculateExp(900)
        );

        assertEquals(
                300,
                ExerciseExpPolicy.calculateExp(1049)
        );

        assertEquals(
                350,
                ExerciseExpPolicy.calculateExp(1050)
        );

        assertEquals(
                350,
                ExerciseExpPolicy.calculateExp(1199)
        );

        assertEquals(
                400,
                ExerciseExpPolicy.calculateExp(1200)
        );

        assertEquals(
                400,
                ExerciseExpPolicy.calculateExp(1349)
        );

        assertEquals(
                450,
                ExerciseExpPolicy.calculateExp(1350)
        );

        assertEquals(
                450,
                ExerciseExpPolicy.calculateExp(1499)
        );
    }

    @Test
    void perfectScoreGives500Exp() {

        assertEquals(
                500,
                ExerciseExpPolicy.calculateExp(1500)
        );
    }

    @Test
    void negativeScoreThrowsException() {

        assertThrows(
                BusinessException.class,
                () -> ExerciseExpPolicy.calculateExp(-1)
        );
    }

    @Test
    void scoreAbove1500ThrowsException() {

        assertThrows(
                BusinessException.class,
                () -> ExerciseExpPolicy.calculateExp(1501)
        );
    }
}