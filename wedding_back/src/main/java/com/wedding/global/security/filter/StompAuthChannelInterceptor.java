package com.wedding.global.security.filter;

import com.wedding.global.util.JWTUtil;
import com.wedding.global.util.RedisTokenService;
import com.wedding.inquiry.service.InquiryAccessService;
import com.wedding.member.dto.MemberDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;
import java.util.Map;

// STOMP CONNECT 프레임에서 JWT를 검증하고(JWTCheckFilter와 동일 로직), SUBSCRIBE 프레임에서
// 그 목적지(방/업체/회원)에 대한 접근 권한을 REST에서 쓰던 InquiryAccessService로 동일하게 재검증한다.
@Component
@RequiredArgsConstructor
@Log4j2
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final RedisTokenService redisTokenService;
    private final InquiryAccessService inquiryAccessService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticate(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        }

        return message;
    }

    // CONNECT 시점: 클라이언트가 "Authorization: Bearer xxx" STOMP 헤더로 보낸 JWT를 검증하고,
    // 이 WebSocket 세션에 로그인 사용자를 붙여둔다 (이후 프레임에서 accessor.getUser()로 재사용됨)
    private void authenticate(StompHeaderAccessor accessor) {

        String authHeader = accessor.getFirstNativeHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AccessDeniedException("WebSocket 인증 토큰이 없습니다.");
        }

        String accessToken = authHeader.substring(7);

        if (redisTokenService.isBlacklisted(accessToken)) {
            throw new AccessDeniedException("로그아웃된 토큰입니다.");
        }

        Map<String, Object> claims = JWTUtil.validateToken(accessToken);

        String email = (String) claims.get("email");
        String pw = (String) claims.get("pw");
        String nickname = (String) claims.get("nickname");
        Boolean social = (Boolean) claims.get("social");
        @SuppressWarnings("unchecked")
        List<String> roleNames = (List<String>) claims.get("roleNames");

        MemberDTO memberDTO = new MemberDTO(email, pw, nickname, social, roleNames);

        Principal principal = new UsernamePasswordAuthenticationToken(memberDTO, pw, memberDTO.getAuthorities());
        accessor.setUser(principal);
    }

    // SUBSCRIBE 시점: 구독하려는 목적지(destination)를 보고 REST와 동일한 권한 규칙을 재검증한다.
    private void authorizeSubscription(StompHeaderAccessor accessor) {

        Principal principal = accessor.getUser();
        if (principal == null) {
            throw new AccessDeniedException("인증되지 않은 구독 요청입니다.");
        }
        String callerEmail = principal.getName();

        String destination = accessor.getDestination();
        if (destination == null) {
            throw new AccessDeniedException("잘못된 구독 요청입니다.");
        }

        if (destination.startsWith("/topic/inquiries/room/")) {
            Long roomId = Long.valueOf(destination.substring("/topic/inquiries/room/".length()));
            inquiryAccessService.requireAccessibleRoom(callerEmail, roomId);

        } else if (destination.startsWith("/topic/inquiries/company/")) {
            Long cmno = Long.valueOf(destination.substring("/topic/inquiries/company/".length()));
            inquiryAccessService.requireCanListCompanyRooms(callerEmail, cmno);

        } else if (destination.startsWith("/topic/inquiries/member/")) {
            String targetEmail = destination.substring("/topic/inquiries/member/".length());
            if (!callerEmail.equals(targetEmail)) {
                throw new AccessDeniedException("본인 알림만 구독할 수 있습니다.");
            }

        } else {
            throw new AccessDeniedException("허용되지 않은 구독 경로입니다.");
        }
    }
}