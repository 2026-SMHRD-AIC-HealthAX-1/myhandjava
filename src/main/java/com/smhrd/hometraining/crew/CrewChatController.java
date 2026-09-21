package com.smhrd.hometraining.crew;

import com.smhrd.hometraining.crew.dto.CrewChatMessageDto;
import com.smhrd.hometraining.crew.dto.CrewChatSendRequest;
import com.smhrd.hometraining.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * [담당] 크루채팅 메시지 "발신"(전송) 전용 STOMP 엔드포인트. 조회는 REST(CrewController
 *        GET /api/crews/me/chat), 신고는 별도 REST(CrewController POST .../report)로 나뉜다.
 * [프론트 연동] ounhome-f/js/crew.js sendCrewChat() → STOMP publish /app/crews/{crewId}/chat.
 * [DB] CrewService.sendChat() → CrewChatMessageRepository → crew_chat_messages 테이블.
 */
@Controller
@RequiredArgsConstructor
public class CrewChatController {

    private final CrewService crewService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/crews/{crewId}/chat")
    public void sendChat(@DestinationVariable Long crewId, CrewChatSendRequest payload, Principal principal) {
        if (!(principal instanceof UsernamePasswordAuthenticationToken auth)
                || !(auth.getPrincipal() instanceof CustomUserPrincipal userPrincipal)) {
            return; // 인증되지 않은 CONNECT — 조용히 무시한다.
        }
        CrewChatMessageDto saved = crewService.sendChat(userPrincipal.getUserId(), crewId, payload.text());
        messagingTemplate.convertAndSend("/topic/crews/" + crewId + "/chat", saved);
    }
}
