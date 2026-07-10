package com.wedding.global.util;


import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.InvalidClaimException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class JWTUtil {

    private static String key = "1234567890123456789012345678901234567890";

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