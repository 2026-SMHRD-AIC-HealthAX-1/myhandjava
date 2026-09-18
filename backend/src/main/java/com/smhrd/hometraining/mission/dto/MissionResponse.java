package com.smhrd.hometraining.mission.dto;

import com.smhrd.hometraining.mission.entity.Mission;

public record MissionResponse(
        Long id,
        String metric,
        String label,
        int target,
        int current,
        int reward,
        boolean achieved,
        boolean claimed
) {
    public static MissionResponse of(Mission m, int current) {
        return new MissionResponse(m.getId(), m.getMetric().name(), m.getLabel(), m.getTarget(),
                Math.min(current, m.getTarget()), m.getReward(), current >= m.getTarget(), m.isClaimed());
    }
}
