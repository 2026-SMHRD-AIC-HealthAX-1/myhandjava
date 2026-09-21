package com.smhrd.hometraining.crew;

import com.smhrd.hometraining.crew.dto.CrewBattlePartyInviteRequest;
import com.smhrd.hometraining.crew.dto.CrewBattlePartyResponseRequest;
import com.smhrd.hometraining.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * 크루대전 파티 초대(매칭 전 팀원 모으기)를 실시간으로 주고받는 STOMP 엔드포인트.
 *
 * 클라이언트 발행: /app/crews/{crewId}/party-invite, /app/crews/{crewId}/party-invite/respond
 * 서버는 초대·응답 모두 대상자 개인 큐(/user/queue/crew-events)로만 전송한다 — 크루 전체
 * 브로드캐스트(/topic/crews/{crewId}/...)가 아니라, 초대받은/초대한 그 한 사람에게만 간다.
 *
 * [담당] 크루대전 시작 전 "파티 맺기"(팀원 초대) 실시간 알림. DB에 저장 안 되는 휘발성 알림.
 * [프론트 연동] ounhome-f/js/crew.js openPartyInvite()/respondPartyInvite() 쪽 STOMP publish.
 * [DB] 없음 — 팀 구성은 각자 클라이언트가 메모리에 들고 있다가 실제 매칭 요청 때만 서버로 감.
 */
@Controller
@RequiredArgsConstructor
public class CrewBattlePartyController {

    private final CrewService crewService;

    @MessageMapping("/crews/{crewId}/party-invite")
    public void invite(
            @DestinationVariable Long crewId,
            CrewBattlePartyInviteRequest payload,
            Principal principal
    ) {
        CustomUserPrincipal userPrincipal = resolve(principal);
        if (userPrincipal == null) return;

        crewService.sendBattlePartyInvite(
                userPrincipal.getUserId(),
                crewId,
                payload.battleSize(),
                payload.inviteeUserIds()
        );
    }

    @MessageMapping("/crews/{crewId}/party-invite/respond")
    public void respond(
            @DestinationVariable Long crewId,
            CrewBattlePartyResponseRequest payload,
            Principal principal
    ) {
        CustomUserPrincipal userPrincipal = resolve(principal);
        if (userPrincipal == null) return;

        crewService.respondBattlePartyInvite(
                userPrincipal.getUserId(),
                crewId,
                payload.inviterUserId(),
                payload.accepted()
        );
    }

    private CustomUserPrincipal resolve(Principal principal) {
        if (!(principal instanceof UsernamePasswordAuthenticationToken auth)
                || !(auth.getPrincipal() instanceof CustomUserPrincipal userPrincipal)) {
            return null; // 인증되지 않은 CONNECT — 조용히 무시한다.
        }
        return userPrincipal;
    }
}
