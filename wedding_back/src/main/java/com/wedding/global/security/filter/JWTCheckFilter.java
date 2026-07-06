package com.wedding.global.security.filter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.google.gson.Gson;
import com.wedding.member.dto.MemberDTO;
import com.wedding.global.util.JWTUtil;
import com.wedding.global.util.RedisTokenService;

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

        if(request.getMethod().equals("GET") && path.startsWith("/api/companies/")){
            return true; // 회사 목록/상세/더미 조회가 JWT 필터에 막혀 프론트 공개 조회가 실패하던 문제 수정
        }

        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException{

        log.info("------------------------JWTCheckFilter------------------");

        String authHeaderStr = request.getHeader("Authorization");

        try {
            //Bearer accestoken...
            String accessToken = authHeaderStr.substring(7);

            // 로그아웃 등으로 블랙리스트에 등록된 토큰이면 거부
            if (redisTokenService.isBlacklisted(accessToken)) {
                throw new RuntimeException("Blacklisted token");
            }

            Map<String, Object> claims = JWTUtil.validateToken(accessToken);

            log.info("JWT claims: " + claims);

            //   filterChain.doFilter(request, response);

            String email = (String) claims.get("email");
            String pw = (String) claims.get("pw");
            String nickname = (String) claims.get("nickname");
            Boolean social = (Boolean) claims.get("social");
            List<String> roleNames = (List<String>) claims.get("roleNames");

            MemberDTO memberDTO = new MemberDTO(email, pw, nickname, social.booleanValue(), roleNames);

            log.info("-----------------------------------");
            log.info(memberDTO);
            log.info(memberDTO.getAuthorities());

            UsernamePasswordAuthenticationToken authenticationToken
                    = new UsernamePasswordAuthenticationToken(memberDTO, pw, memberDTO.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

            filterChain.doFilter(request, response);

        }catch(Exception e){

            log.error("JWT Check Error..............");
            log.error(e.getMessage());

            // 수정: 토큰 검증 실패 시 401 상태코드를 명시적으로 지정
            // (이게 없으면 기본값 200으로 응답이 나가서, 프론트 axios가 이 응답을
            //  "정상 성공"으로 착각하고 {"error": "..."} 객체를 그대로 데이터로 써버림)
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            Gson gson = new Gson();
            String msg = gson.toJson(Map.of("error", "ERROR_ACCESS_TOKEN"));

            response.setContentType("application/json");
            PrintWriter printWriter = response.getWriter();
            printWriter.println(msg);
            printWriter.close();

        }
    }

}
