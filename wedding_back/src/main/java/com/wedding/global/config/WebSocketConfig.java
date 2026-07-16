package com.wedding.global.config;

import com.wedding.global.security.filter.StompAuthChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    // 클라이언트가 WebSocket 연결을 시작하는 주소. SockJS를 붙여서, 브라우저/네트워크가
    // 순정 WebSocket을 못 쓰는 환경이어도 자동으로 HTTP 폴링 방식으로 대체 접속되게 한다.
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-inquiry")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    // "/topic"으로 시작하는 주소를 구독한 클라이언트들에게 메시지를 뿌려주는 역할을 활성화한다.
    // (클라이언트 -> 서버로 STOMP SEND를 보내는 기능은 이번 설계에서 안 쓰기로 했으니
    //  setApplicationDestinationPrefixes("/app")는 필요 없음 - 전송은 계속 REST로)
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
    }

    // 서버로 들어오는 모든 STOMP 프레임(CONNECT/SUBSCRIBE 등)이 이 인터셉터를 거치도록 등록
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }

}
