package com.smhrd.hometraining.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * 크루채팅용 STOMP 엔드포인트.
 * 클라이언트 구독: /topic/crews/{crewId}/chat
 * 클라이언트 발행: /app/crews/{crewId}/chat  →  CrewChatController가 처리
 * CONNECT 프레임에 Authorization: Bearer &lt;JWT&gt; 네이티브 헤더를 실어 보내야 한다(StompAuthChannelInterceptor 참고).
 *
 * [담당] 실시간 통신(크루채팅/크루대전 렙 반영/파티 초대/멤버십 변경) 전부의 WebSocket 인프라 설정.
 * [DB] 없음 — 순수 인프라 설정. 실제 메시지 저장은 CrewChatController 등 개별 컨트롤러가 담당.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // /queue는 유저 단위 알림(convertAndSendToUser, 가입 승인 알림 등)에 쓴다 — 이게 없으면
        // /user/**로 보낸 메시지가 내부적으로 /queue/**로 재작성된 뒤 브로커가 그 prefix를
        // 모른다고 그냥 버려서, 구독 중이어도 메시지가 조용히 씹힌다.
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }
}
