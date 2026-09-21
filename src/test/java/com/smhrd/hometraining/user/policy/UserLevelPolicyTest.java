package com.smhrd.hometraining.user.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UserLevelPolicyTest {

    @Test
    void level1To10Requires100Exp() {

        assertEquals(
                100,
                UserLevelPolicy.getRequiredExp(1)
        );

        assertEquals(
                100,
                UserLevelPolicy.getRequiredExp(10)
        );
    }

    @Test
    void level11To20Requires150Exp() {

        assertEquals(
                150,
                UserLevelPolicy.getRequiredExp(11)
        );

        assertEquals(
                150,
                UserLevelPolicy.getRequiredExp(20)
        );
    }

    @Test
    void requiredExpIncreasesEvery10Levels() {

        assertEquals(
                200,
                UserLevelPolicy.getRequiredExp(21)
        );

        assertEquals(
                250,
                UserLevelPolicy.getRequiredExp(31)
        );

        assertEquals(
                300,
                UserLevelPolicy.getRequiredExp(41)
        );
    }

    @Test
    void highLevelRequiredExpIsCalculatedCorrectly() {

        assertEquals(
                2450,
                UserLevelPolicy.getRequiredExp(471)
        );

        assertEquals(
                2450,
                UserLevelPolicy.getRequiredExp(480)
        );

        assertEquals(
                2500,
                UserLevelPolicy.getRequiredExp(481)
        );

        assertEquals(
                2500,
                UserLevelPolicy.getRequiredExp(500)
        );
    }

    @Test
    void requiredExpDoesNotExceed2500() {

        assertEquals(
                2500,
                UserLevelPolicy.getRequiredExp(490)
        );

        assertEquals(
                2500,
                UserLevelPolicy.getRequiredExp(491)
        );

        assertEquals(
                2500,
                UserLevelPolicy.getRequiredExp(500)
        );
    }

    @Test
    void maxLevelCanBeChecked() {

        assertFalse(
                UserLevelPolicy.isMaxLevel(499)
        );

        assertTrue(
                UserLevelPolicy.isMaxLevel(500)
        );
    }

    @Test
    void levelBelow1ThrowsException() {

        assertThrows(
                IllegalArgumentException.class,
                () -> UserLevelPolicy.getRequiredExp(0)
        );
    }

    @Test
    void levelAbove500ThrowsException() {

        assertThrows(
                IllegalArgumentException.class,
                () -> UserLevelPolicy.getRequiredExp(501)
        );
    }
}