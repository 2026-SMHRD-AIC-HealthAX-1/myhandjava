package com.smhrd.hometraining.config;

import com.smhrd.hometraining.security.CustomUserDetailsService;
import com.smhrd.hometraining.security.CustomUserPrincipal;
import com.smhrd.hometraining.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

/** STOMP CONNECT 프레임의 Authorization 헤더(Bearer 토큰)로 채팅 세션 사용자를 인증한다. */
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        // StompHeaderAccessor.wrap(message)로 얻은 접근자는 message와 분리된 복사본이라
        // setUser()로 수정해도 반환되는 message에는 반영되지 않는다(그래서 CONNECT 인증이 저장은커녕
        // 브로드캐스트도 못 태우고 계속 조용히 씹혔다). getAccessor()로 이 메시지에 실제로 붙어있는
        // (leaveMutable) 접근자를 가져와야 setUser()가 그대로 반영된다.
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String header = accessor.getFirstNativeHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                if (jwtTokenProvider.isValid(token)) {
                    Long userId = jwtTokenProvider.getUserId(token);
                    CustomUserPrincipal principal = (CustomUserPrincipal) userDetailsService.loadUserById(userId);
                    var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                    accessor.setUser(auth);
                }
            }
        }
        return message;
    }
}
