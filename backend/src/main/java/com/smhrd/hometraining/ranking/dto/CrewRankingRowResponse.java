package com.smhrd.hometraining.ranking.dto;

public record CrewRankingRowResponse(
        int rank,
        Long crewId,
        String crewName,
        int level,
        long totalScore,
        String region
) {}
