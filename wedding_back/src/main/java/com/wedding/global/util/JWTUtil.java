package com.wedding.global.util;


import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.InvalidClaimException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.extern.log4j.Log4j2;

// 재원 수정 - 서명 키를 소스코드에 하드코딩해두면, 코드만 봐도(또는 유출되면) 그 값으로
// 누구나 유효한 토큰을 위조해 로그인 없이 API를 호출할 수 있다. 다른 비밀값들(openai.api-key
// 등)과 동일하게 application.properties의 ${JWT_SECRET:} 방식으로 옮기되, generateToken/
// validateToken은 기존 호출부(AuthRefreshController 등 static 호출)를 그대로 유지하기 위해
// static으로 남기고, 스프링이 기동 시점에 setter로 한 번만 주입해준다.
@Component
@Log4j2
public class JWTUtil {

    private static String key;

    @Value("${jwt.secret}")
    public void setKey(String injectedKey) {
        JWTUtil.key = injectedKey;
    }
    

    // 토큰 만료시간(분) 표준값 - 업계에서 흔히 쓰는 범위(Access 30분~1시간, Refresh 1주~1개월)에 맞춤
    // 여러 곳에서 60*24 같은 매직넘버로 중복 박아두던 걸 여기 한 곳으로 모음
    public static final int ACCESS_TOKEN_MINUTES = 60;                    // 1시간
    public static final int REFRESH_TOKEN_MINUTES_DEFAULT = 60 * 24 * 7;  // 7일 (로그인 유지 미체크)
    public static final int REFRESH_TOKEN_MINUTES_REMEMBER = 60 * 24 * 30; // 30일 (로그인 유지 체크)

    public static String generateToken(Map<String, Object> valueMap, int min){

        SecretKey key = null;

        try{
            key = Keys.hmacShaKeyFor(JWTUtil.key.getBytes("UTF-8"));

        }catch(Exception e){
            throw new RuntimeException(e.getMessage());
        }

        String jwtStr = Jwts.builder()
                .setHeader(Map.of("typ","JWT"))
                .setClaims(valueMap)
                .setIssuedAt(Date.from(ZonedDateTime.now().toInstant()))
                .setExpiration(Date.from(ZonedDateTime.now().plusMinutes(min).toInstant()))
                .signWith(key)
                .compact();

        return jwtStr;
    }

    public static Map<String, Object> validateToken(String token) {

        Map<String, Object> claim = null;

        try{

            SecretKey key = Keys.hmacShaKeyFor(JWTUtil.key.getBytes("UTF-8"));

            claim = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token) // 파싱 및 검증, 실패 시 에러
                    .getBody();

        }catch(MalformedJwtException malformedJwtException){
            throw new CustomJWTException("MalFormed");
        }catch(ExpiredJwtException expiredJwtException){
            throw new CustomJWTException("Expired");
        }catch(InvalidClaimException invalidClaimException){
            throw new CustomJWTException("Invalid");
        }catch(JwtException jwtException){
            throw new CustomJWTException("JWTError");
        }catch(Exception e){
            throw new CustomJWTException("Error");
        }
        return claim;
    }

}