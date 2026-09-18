package com.smhrd.hometraining.exercise.policy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.smhrd.hometraining.common.exception.BusinessException;

class ExerciseScorePolicyTest {

    @Test
    void allPerfectIs1500Points() {

        int score =
                ExerciseScorePolicy.calculateScore(
                        15,
                        0,
                        0,
                        0
                );

        assertEquals(
                1500,
                score
        );
    }

    @Test
    void mixedGradesCalculateCorrectScore() {

        int score =
                ExerciseScorePolicy.calculateScore(
                        5,
                        4,
                        3,
                        3
                );

        assertEquals(
                970,
                score
        );
    }

    @Test
    void fewerThan15RepsCanBeCalculated() {

        int score =
                ExerciseScorePolicy.calculateScore(
                        3,
                        2,
                        1,
                        1
                );

        assertEquals(
                510,
                score
        );
    }

    @Test
    void matchingRepsPassValidation() {

        assertDoesNotThrow(
                () -> ExerciseScorePolicy.validateRepsMatch(
                        15,
                        5,
                        4,
                        3,
                        3
                )
        );
    }

    @Test
    void mismatchedRepsThrowException() {

        assertThrows(
                BusinessException.class,
                () -> ExerciseScorePolicy.validateRepsMatch(
                        15,
                        5,
                        4,
                        3,
                        2
                )
        );
    }

    @Test
    void moreThan15GradesThrowException() {

        assertThrows(
                BusinessException.class,
                () -> ExerciseScorePolicy.calculateScore(
                        16,
                        0,
                        0,
                        0
                )
        );
    }

    @Test
    void negativeGradeCountThrowsException() {

        assertThrows(
                BusinessException.class,
                () -> ExerciseScorePolicy.calculateScore(
                        -1,
                        5,
                        5,
                        5
                )
        );
    }
}