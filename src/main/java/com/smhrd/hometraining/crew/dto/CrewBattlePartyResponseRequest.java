package com.smhrd.hometraining.crew.dto;

/** 크루대전 파티 초대에 대한 수락/거절 — /app/crews/{crewId}/party-invite/respond 로 들어오는 페이로드. */
public record CrewBattlePartyResponseRequest(
        Long inviterUserId,
        boolean accepted
) {
}
