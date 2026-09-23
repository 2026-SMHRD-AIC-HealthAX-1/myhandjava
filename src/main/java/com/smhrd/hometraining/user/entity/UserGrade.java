package com.smhrd.hometraining.user.entity;

public enum UserGrade {

    /**
     * 순위 도전에 한 번도 참여하지 않은 상태입니다.
     */
    UNRANKED("언랭크", -1),

    IRON("아이언", 0),
    BRONZE("브론즈", 150_000),
    SILVER("실버", 350_000),
    GOLD("골드", 650_000),
    PLATINUM("플래티넘", 1_100_000),
    EMERALD("에메랄드", 1_800_000),
    DIAMOND("다이아몬드", 2_900_000),
    MASTER("마스터", 4_600_000),
    GRANDMASTER("그랜드마스터", 7_300_000),
    CHALLENGER("챌린저", 11_500_000);

    private final String koreanName;

    /**
     * 이 등급으로 승급하는 데 필요한 순위 도전 누적 점수입니다.
     *
     * UNRANKED는 점수 구간이 아니라 "참여 전" 상태라 사용하지 않습니다.
     */
    private final long minRankedScore;

    UserGrade(String koreanName, long minRankedScore) {
        this.koreanName = koreanName;
        this.minRankedScore = minRankedScore;
    }

    public String getKoreanName() {
        return koreanName;
    }

    /**
     * 순위 도전 누적 점수를 기준으로 등급을 계산합니다.
     *
     * 순위 도전에 한 번도 참여하지 않았다면 UNRANKED를 반환하고, 참여했다면
     * 누적 점수가 넘은 구간 중 가장 높은 등급을 반환합니다(최소 아이언).
     */
    public static UserGrade forRankedScore(
            boolean hasJoinedRankedChallenge,
            long rankedScoreTotal
    ) {

        if (!hasJoinedRankedChallenge) {
            return UNRANKED;
        }

        UserGrade result = IRON;

        for (UserGrade candidate : values()) {

            if (candidate == UNRANKED) {
                continue;
            }

            if (rankedScoreTotal >= candidate.minRankedScore) {
                result = candidate;
            }
        }

        return result;
    }
}
