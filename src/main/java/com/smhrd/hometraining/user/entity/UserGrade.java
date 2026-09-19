package com.smhrd.hometraining.user.entity;

public enum UserGrade {

    IRON("아이언"),
    BRONZE("브론즈"),
    SILVER("실버"),
    GOLD("골드"),
    PLATINUM("플래티넘"),
    EMERALD("에메랄드"),
    DIAMOND("다이아몬드"),
    MASTER("마스터"),
    GRANDMASTER("그랜드마스터"),
    CHALLENGER("챌린저");

    private final String koreanName;

    UserGrade(String koreanName) {
        this.koreanName = koreanName;
    }

    public String getKoreanName() {
        return koreanName;
    }

    public UserGrade next() {

        int nextIndex = ordinal() + 1;

        if (nextIndex >= values().length) {
            return CHALLENGER;
        }

        return values()[nextIndex];
    }

    public boolean isHighestGrade() {
        return this == CHALLENGER;
    }
}