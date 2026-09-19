package com.smhrd.hometraining.ranking.dto;

public record RankingRowResponse(
        int rank,
        Long userId,
        String nickname,
        int level,
        long score,
        boolean me
) {}
