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
