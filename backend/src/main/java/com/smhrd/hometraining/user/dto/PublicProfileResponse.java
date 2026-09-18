package com.smhrd.hometraining.user.dto;

import java.util.List;

/** 랭킹 단상 아바타 클릭 시 보여주는 다른 사용자의 "공개 프로필" 요약. isPublic이 false면
 *  nickname만 채우고 나머지는 비워서(0/null) 보낸다 — 실제 노출 여부는 프론트가 이 필드로 판단한다. */
public record PublicProfileResponse(
        boolean isPublic,
        String nickname,
        int level,
        String bio,
        long totalScore,
        long perfectCount,
        long greatCount,
        long goodCount,
        long missCount,
        List<ExerciseCount> exerciseCounts
) {
    public record ExerciseCount(String exerciseType, long count) {}
}
