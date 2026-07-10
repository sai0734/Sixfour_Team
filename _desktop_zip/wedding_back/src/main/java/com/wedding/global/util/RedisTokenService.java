package com.wedding.global.util;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// 엑셀 설계의 Redis 스펙 그대로 구현
// refresh:{email}   -> refresh 토큰 문자열, TTL 7일(자동로그인 30일)
// blacklist:{token} -> "1", TTL은 그 토큰의 남은 만료시간까지
//
// Redis 서버가 없는 환경(예: 발표/시연용 컴퓨터)에서도 로그인/회원가입 등
// 핵심 기능이 죽지 않도록, 모든 메서드는 Redis 연결 실패를 조용히 흡수한다.
// (이 경우 refresh 토큰 갱신/로그아웃 즉시반영/블랙리스트 기능만 비활성화됨)
@Service
@RequiredArgsConstructor
@Log4j2
public class RedisTokenService {

  private final RedisTemplate<String, String> redisTemplate;

  private static final String REFRESH_PREFIX = "refresh:";
  private static final String BLACKLIST_PREFIX = "blacklist:";

  public void saveRefreshToken(String email, String refreshToken, boolean rememberMe) {

    long days = rememberMe ? 30 : 7;

    try {
      redisTemplate.opsForValue().set(REFRESH_PREFIX + email, refreshToken, Duration.ofDays(days));
    } catch (Exception e) {
      log.warn("Redis 사용 불가 - refresh 토큰 저장 생략 (email=" + email + ")");
    }
  }

  public String getRefreshToken(String email) {

    try {
      return redisTemplate.opsForValue().get(REFRESH_PREFIX + email);
    } catch (Exception e) {
      log.warn("Redis 사용 불가 - refresh 토큰 조회 생략 (email=" + email + ")");
      return null;
    }
  }

  public void deleteRefreshToken(String email) {

    try {
      redisTemplate.delete(REFRESH_PREFIX + email);
    } catch (Exception e) {
      log.warn("Redis 사용 불가 - refresh 토큰 삭제 생략 (email=" + email + ")");
    }
  }

  public void addToBlacklist(String accessToken, long remainingSeconds) {

    if (remainingSeconds <= 0) {
      return;
    }

    try {
      redisTemplate.opsForValue().set(BLACKLIST_PREFIX + accessToken, "1", Duration.ofSeconds(remainingSeconds));
    } catch (Exception e) {
      log.warn("Redis 사용 불가 - 블랙리스트 등록 생략");
    }
  }

  public boolean isBlacklisted(String accessToken) {

    try {
      Boolean exists = redisTemplate.hasKey(BLACKLIST_PREFIX + accessToken);
      return Boolean.TRUE.equals(exists);
    } catch (Exception e) {
      log.warn("Redis 사용 불가 - 블랙리스트 확인 생략 (차단하지 않고 통과)");
      return false;
    }
  }

}