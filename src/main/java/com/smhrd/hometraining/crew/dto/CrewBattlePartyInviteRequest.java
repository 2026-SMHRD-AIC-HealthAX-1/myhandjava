package com.smhrd.hometraining.crew.dto;

import java.util.List;

/** 크루대전 파티 초대 요청 — /app/crews/{crewId}/party-invite 로 들어오는 페이로드. */
public record CrewBattlePartyInviteRequest(
        int battleSize,
        List<Long> inviteeUserIds
) {
}
