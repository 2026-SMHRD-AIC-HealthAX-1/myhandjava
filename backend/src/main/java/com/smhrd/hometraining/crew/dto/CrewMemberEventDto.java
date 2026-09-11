package com.smhrd.hometraining.crew.dto;

/** 크루원 강퇴 등 멤버십 변경을 /topic/crews/{crewId}/members 로 실시간 브로드캐스트할 때 쓰는 페이로드. */
public record CrewMemberEventDto(
        String type, // "KICKED"
        Long crewId,
        Long targetUserId,
        String targetNickname
) {
}
