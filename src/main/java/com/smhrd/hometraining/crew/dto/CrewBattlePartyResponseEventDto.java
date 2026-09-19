package com.smhrd.hometraining.crew.dto;

/** 파티 초대 수락/거절 응답을 초대한 사람의 개인 큐(/user/queue/crew-events)로 전달하는 이벤트. */
public record CrewBattlePartyResponseEventDto(
        String type, // "BATTLE_PARTY_RESPONSE"
        Long crewId,
        Long responderUserId,
        String responderNickname,
        boolean accepted
) {
}
