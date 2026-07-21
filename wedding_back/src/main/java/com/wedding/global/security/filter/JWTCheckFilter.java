package com.wedding.global.security.filter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.google.gson.Gson;
import com.wedding.member.domain.Member;
import com.wedding.member.dto.MemberDTO;
import com.wedding.global.util.JWTUtil;
import com.wedding.global.util.RedisTokenService;
import com.wedding.member.repository.MemberDetailRepository;
import com.wedding.member.repository.MemberRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
public class JWTCheckFilter extends OncePerRequestFilter{

    private final RedisTokenService redisTokenService;
    private final MemberRepository memberRepository;
    private final MemberDetailRepository memberDetailRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException{
        // Preflight요청은 체크하지 않음
        // 어떠한 요청을 보낼껀데, 너희 서버 잘돌아가니?
        if(request.getMethod().equals("OPTIONS")){
            return true;
        }

        String path = request.getRequestURI();
        String method = request.getMethod();

        log.info("check uri......................."+path);

        // WebSocket(SockJS) 핸드셰이크 경로는 이 필터에서 제외 - 인증은 STOMP CONNECT 프레임에서
        // StompAuthChannelInterceptor가 별도로 처리한다 (핸드셰이크 자체엔 Authorization 헤더가 없음)
        if(path.startsWith("/ws-inquiry")) {
            return true;
        }

        //api/auth/ 경로의 호출은 체크하지 않음
        if(path.startsWith("/api/auth/")) {
            return true;
        }

        // 이미지 조회 경로는 체크하지 않는다면 ()
        if(method.equals("GET")) {

            if(path.equals("/api/product/list")) return true;

            if(path.startsWith("/api/product/view/")) return true;

            if(path.startsWith("/api/product/")) {

                String rest = path.substring("/api/product/".length());

                // 슬래시 없이 pno만 있는 경우: /api/product/21
                if(!rest.contains("/")) return true;

                // pno/options 형태: /api/product/21/options
                if(rest.endsWith("/options")) return true;

                // pno/options/price 형태: /api/product/21/options/price
                if(rest.endsWith("/options/price")) return true;

                if(rest.endsWith("/reviews")) return true;

                if(rest.endsWith("/qna")) return true;
            }
        }

        if(path.startsWith("/api/companies/images/view/")){
            return true;
        }

        // 게시글/댓글 첨부 이미지·동영상 조회 - <img>/<video> 태그는 Authorization 헤더를 못 보내므로 예외 처리
        // (댓글 이미지도 /api/commentimages/{id} 업로드일 뿐, 실제 파일 서빙은 이 경로를 그대로 재사용함)
        if(method.equals("GET") && path.startsWith("/api/boardimages/view/")){
            return true;
        }

        if(request.getMethod().equals("GET") && path.startsWith("/api/companies/")
                && !path.equals("/api/companies/managers")
                && !path.equals("/api/companies/my-managed")
                && !path.equals("/api/companies/managed-by")){
            return true; // 회사 목록/상세/더미 조회가 JWT 필터에 막혀 프론트 공개 조회가 실패하던 문제 수정
            // (/managers, /my-managed, /managed-by는 로그인/권한 필요 - JWT를 반드시 검증해야 함)
        }

        // 게시판 목록/상세 조회는 비로그인 사용자도 볼 수 있어야 함 (커뮤니티 글은 공개 조회)
        if(method.equals("GET") && path.equals("/api/boards/")) {
            return true;
        }
        if(method.equals("GET") && path.equals("/api/boards/best")) {
            return true;
        }
        if(method.equals("GET") && path.matches("/api/boards/\\d+")) {
            return true;
        }
        // AI 한줄요약도 게시글 조회의 일부 - 비로그인 사용자도 볼 수 있어야 함
        if(method.equals("GET") && path.matches("/api/boards/\\d+/summary")) {
            return true;
        }

        // 댓글 목록 조회도 공개 (게시글을 보면 댓글도 같이 보여야 하므로)
        if(method.equals("GET") && path.matches("/api/comments/board/\\d+")) {
            return true;
        }

        // FAQ 목록/상세 조회도 공개
        if(method.equals("GET") && path.equals("/api/faqs/")) {
            return true;
        }
        if(method.equals("GET") && path.matches("/api/faqs/\\d+")) {
            return true;
        }

        // 재원 추가 - 업체 상세페이지 "결제 횟수" 표시는 비로그인 사용자도 볼 수 있어야 함
        if(method.equals("GET") && path.matches("/api/reservations/company/\\d+/payment-count")) {
            return true;
        }

        // 재원 추가 - 예약 날짜 중복 확인도 비로그인 사용자가 예약 화면 들어가기 전에 볼 수 있어야 함
        if(method.equals("GET") && path.matches("/api/reservations/company/\\d+/availability")) {
            return true;
        }

        // 재원 추가 - AI 웨딩플랜은 전부 비로그인 허용 목록이지만, 로그인 토큰이 있으면
        // (막지는 않고) 파싱만 해서 세션에 회원 이메일을 남길 수 있게 한다.
        if (isAiPlanPublicPath(path, method)) {
            tryPopulateOptionalAuthentication(request);
            return true;
        }

        // AI 드레스 — 드레스 목록은 공개 조회 (합성·내 사진은 JWT 필요)
        if(method.equals("GET") && path.equals("/api/ai-dress/dresses")) {
            return true;
        }

        return false;
    }

    // aiplan 관련 공개(비로그인 허용) 경로 모음 - 흩어져 있던 조건을 한 곳으로 통합
    private boolean isAiPlanPublicPath(String path, String method) {
        if (method.equals("GET")) {
            if (path.equals("/api/aiplan/quick")) return true;
            if (path.equals("/api/aiplan/detail")) return true;
            if (path.matches("/api/aiplan/session/\\d+")) return true;
            if (path.matches("/api/aiplan/session/\\d+/history")) return true;
        }
        if (method.equals("POST")) {
            if (path.equals("/api/aiplan/ai")) return true;
            if (path.equals("/api/aiplan/refine")) return true;
            if (path.equals("/api/aiplan/slot")) return true;
            if (path.matches("/api/aiplan/session/\\d+/turn/\\d+")) return true;
        }
        return false;
    }

    // aiplan 경로는 비로그인 허용이라 여기선 절대 막지 않는다. 다만 로그인한 회원이 쓰는 경우
    // AiPlanSession에 이메일을 남기고 싶어서, 토큰이 있으면 doFilterInternal의 파싱 로직과
    // 동일하게 SecurityContext를 채워둔다. 토큰이 없거나 만료/위조돼도 조용히 넘어간다
    // (401을 보내거나 여기서 return하지 않음 - shouldNotFilter 흐름을 끊으면 안 되므로).
    private void tryPopulateOptionalAuthentication(HttpServletRequest request) {
        String authHeaderStr = request.getHeader("Authorization");
        if (authHeaderStr == null || !authHeaderStr.startsWith("Bearer ")) {
            return;
        }

        try {
            String accessToken = authHeaderStr.substring(7);

            if (redisTokenService.isBlacklisted(accessToken)) {
                return;
            }

            Map<String, Object> claims = JWTUtil.validateToken(accessToken);

            String email = (String) claims.get("email");
            String pw = (String) claims.get("pw");
            String nickname = (String) claims.get("nickname");
            Boolean social = (Boolean) claims.get("social");
            List<String> roleNames = (List<String>) claims.get("roleNames");

            MemberDTO memberDTO = new MemberDTO(email, pw, nickname, social.booleanValue(), roleNames);

            UsernamePasswordAuthenticationToken authenticationToken
                    = new UsernamePasswordAuthenticationToken(memberDTO, pw, memberDTO.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        } catch (Exception e) {
            log.debug("aiplan optional auth 파싱 실패(무시하고 통과): " + e.getMessage());
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException{

        log.info("------------------------JWTCheckFilter------------------");

        String authHeaderStr = request.getHeader("Authorization");

        // try 안에는 토큰 검증 로직만 둔다 - doFilter()를 try 밖에 둬야 인증 통과 이후
        // 컨트롤러/서비스에서 발생하는 예외를 이 catch가 401로 잘못 삼키지 않는다
        try {
            if (authHeaderStr == null || !authHeaderStr.startsWith("Bearer ")) {
                throw new RuntimeException("Missing or invalid Authorization header");
            }

            //Bearer accestoken...
            String accessToken = authHeaderStr.substring(7);

            // 로그아웃 등으로 블랙리스트에 등록된 토큰이면 거부
            if (redisTokenService.isBlacklisted(accessToken)) {
                throw new RuntimeException("Blacklisted token");
            }

            Map<String, Object> claims = JWTUtil.validateToken(accessToken);

            log.info("JWT claims: " + claims);

            String email = (String) claims.get("email");
            String pw = (String) claims.get("pw");
            String nickname = (String) claims.get("nickname");
            Boolean social = (Boolean) claims.get("social");
            List<String> roleNames = (List<String>) claims.get("roleNames");

            // 이미 발급된 accessToken이 살아있어도, 그 사이 관리자가 정지/휴면 처리했으면 즉시 차단
            Optional<Member> memberOpt = memberRepository.findById(email);

            if (memberOpt.isPresent()) {
                String status = memberOpt.get().getStatus();

                if ("BLACKLIST".equals(status) || "DORMANT".equals(status) || "WITHDRAWN".equals(status)) {
                    sendBlockedResponse(response, status);
                    return;
                }

                // 소셜(카카오) 최초가입 후 추가정보(이름/전화/주소/약관동의)를 아직 안 넣은 상태면,
                // 그 추가정보를 제출하는 API 자체는 당연히 허용하고 그 외 모든 API는 차단
                String path = request.getRequestURI();
                boolean isSocialCompleteRequest = "/api/member/social-complete".equals(path);

                if (memberOpt.get().isSocial() && !isSocialCompleteRequest) {
                    boolean hasProfile = memberDetailRepository.getByMemberEmail(email).isPresent();

                    if (!hasProfile) {
                        sendIncompleteProfileResponse(response);
                        return;
                    }
                }
            }

            MemberDTO memberDTO = new MemberDTO(email, pw, nickname, social.booleanValue(), roleNames);

            log.info("-----------------------------------");
            log.info(memberDTO);
            log.info(memberDTO.getAuthorities());

            UsernamePasswordAuthenticationToken authenticationToken
                    = new UsernamePasswordAuthenticationToken(memberDTO, pw, memberDTO.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        }catch(Exception e){

            log.error("JWT Check Error..............");
            log.error(e.getMessage());

            // 상태코드를 명시하지 않으면 기본값 200이 나가서, 프론트 axios가 이 응답을
            // "정상 성공"으로 착각하고 {"error": "..."} 객체를 그대로 데이터로 써버린다
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            Gson gson = new Gson();
            String msg = gson.toJson(Map.of("error", "ERROR_ACCESS_TOKEN"));

            response.setContentType("application/json");
            PrintWriter printWriter = response.getWriter();
            printWriter.println(msg);
            printWriter.close();

            // 여기서 return하지 않으면 catch 블록이 끝난 뒤 아래의 filterChain.doFilter()가
            // 그대로 실행되어, 이미 401 응답을 다 쓴 요청이 컨트롤러까지 넘어가버린다.
            return;

        }

        // 토큰 검증(위 try)이 예외 없이 끝났을 때만 여기 도달한다.
        // 이 아래에서 발생하는 예외는 더 이상 위 catch가 삼키지 않고 그대로 전파된다.
        filterChain.doFilter(request, response);
    }

    // 정지/휴면/탈퇴 회원의 요청을 즉시 차단할 때 쓰는 응답 (일반 토큰 오류와 구분되는 전용 에러코드)
    private void sendBlockedResponse(HttpServletResponse response, String status) throws IOException {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        String errorCode = "WITHDRAWN".equals(status) ? "ERROR_ACCOUNT_WITHDRAWN" : "ERROR_ACCOUNT_BLOCKED";

        Gson gson = new Gson();
        String msg = gson.toJson(Map.of("error", errorCode));

        response.setContentType("application/json");
        PrintWriter printWriter = response.getWriter();
        printWriter.println(msg);
        printWriter.close();
    }

    // 소셜 최초가입인데 추가정보(SocialCompletePage) 입력을 안 끝낸 상태로 다른 API를 호출할 때 쓰는 응답
    private void sendIncompleteProfileResponse(HttpServletResponse response) throws IOException {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        Gson gson = new Gson();
        String msg = gson.toJson(Map.of("error", "ERROR_PROFILE_INCOMPLETE"));

        response.setContentType("application/json");
        PrintWriter printWriter = response.getWriter();
        printWriter.println(msg);
        printWriter.close();
    }

}