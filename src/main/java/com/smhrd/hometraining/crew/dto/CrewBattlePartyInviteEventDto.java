package com.smhrd.hometraining.crew.dto;

/** 크루대전 파티 초대를 받은 사람에게 개인 큐(/user/queue/crew-events)로 보내는 이벤트. */
public record CrewBattlePartyInviteEventDto(
        String type, // "BATTLE_PARTY_INVITE"
        Long crewId,
        int battleSize,
        Long inviterUserId,
        String inviterNickname
) {
}
