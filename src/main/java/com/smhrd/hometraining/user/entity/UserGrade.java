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

    // 레벨을 10단위로 끊어 등급을 정한다: 1~10 아이언, 11~20 브론즈, ... 91~100(이상) 챌린저.
    // 100레벨을 넘어가도 정의된 등급이 없으므로 마지막 등급(챌린저)에 그대로 머문다.
    public static UserGrade forLevel(int level) {
        int bucket = Math.max(0, (level - 1) / 10);
        int index = Math.min(values().length - 1, bucket);
        return values()[index];
    }
}