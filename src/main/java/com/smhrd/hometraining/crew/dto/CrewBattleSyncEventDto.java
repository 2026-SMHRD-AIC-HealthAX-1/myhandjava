package com.smhrd.hometraining.crew.dto;

import com.smhrd.hometraining.crew.battle.dto.CrewBattleDto;

/**
 * 크루대전 자동 매칭을 신청/매칭한 순간, 신청자 본인을 제외한 나머지 참가자들에게
 * 개인 큐(/user/queue/crew-events)로 보내는 이벤트.
 *
 * 신청자 본인은 REST 응답으로 이미 이 battle 데이터를 받아 화면을 전환하지만, 같은 파티의
 * 나머지 팀원들(그리고 매칭이 잡혔다면 상대 크루 전원)은 이 이벤트를 받아야만 대기/대전
 * 화면으로 따라 들어갈 수 있다 — CrewBattleService.request() 참고.
 */
public record CrewBattleSyncEventDto(
        String type, // "BATTLE_SYNC"
        CrewBattleDto.Response battle
) {
}
