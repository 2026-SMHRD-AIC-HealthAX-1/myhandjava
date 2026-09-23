package com.smhrd.hometraining.user.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class UserGradeTest {

    @Test
    void neverJoinedIsUnranked() {

        assertEquals(
                UserGrade.UNRANKED,
                UserGrade.forRankedScore(false, 0)
        );

        /*
         * 참여 전이라면 누적 점수가 남아있어도(예: 데이터 이관 등) UNRANKED입니다.
         */
        assertEquals(
                UserGrade.UNRANKED,
                UserGrade.forRankedScore(false, 999_999_999)
        );
    }

    @Test
    void firstParticipationGrantsIronEvenWithZeroScore() {

        assertEquals(
                UserGrade.IRON,
                UserGrade.forRankedScore(true, 0)
        );
    }

    @Test
    void scoreJustBelowThresholdStaysInLowerGrade() {

        assertEquals(
                UserGrade.IRON,
                UserGrade.forRankedScore(true, 149_999)
        );

        assertEquals(
                UserGrade.BRONZE,
                UserGrade.forRankedScore(true, 150_000)
        );
    }

    @Test
    void everyGradeBoundaryMapsCorrectly() {

        assertEquals(UserGrade.BRONZE, UserGrade.forRankedScore(true, 150_000));
        assertEquals(UserGrade.SILVER, UserGrade.forRankedScore(true, 350_000));
        assertEquals(UserGrade.GOLD, UserGrade.forRankedScore(true, 650_000));
        assertEquals(UserGrade.PLATINUM, UserGrade.forRankedScore(true, 1_100_000));
        assertEquals(UserGrade.EMERALD, UserGrade.forRankedScore(true, 1_800_000));
        assertEquals(UserGrade.DIAMOND, UserGrade.forRankedScore(true, 2_900_000));
        assertEquals(UserGrade.MASTER, UserGrade.forRankedScore(true, 4_600_000));
        assertEquals(UserGrade.GRANDMASTER, UserGrade.forRankedScore(true, 7_300_000));
        assertEquals(UserGrade.CHALLENGER, UserGrade.forRankedScore(true, 11_500_000));
    }

    @Test
    void scoreFarAboveChallengerStaysAtChallenger() {

        assertEquals(
                UserGrade.CHALLENGER,
                UserGrade.forRankedScore(true, 50_000_000)
        );
    }
}
