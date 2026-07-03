package com.wedding.global.config;

import org.springframework.context.annotation.Configuration;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;

// 전역 설정(@Enable___ 계열 어노테이션)은 이 클래스에 모아서 관리합니다.
// WeddingApplication.java(메인 클래스)에는 추가하지 마세요 - 중복 등록되면 부팅 실패합니다.
@Configuration
public class RootConfig {

  @Bean
  public ModelMapper getMapper() {
    ModelMapper modelMapper = new ModelMapper();
    modelMapper.getConfiguration()
            .setFieldMatchingEnabled(true)
            .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE)
            .setMatchingStrategy(MatchingStrategies.LOOSE)
            // DTO에서 안 보낸 필드(null)가 엔티티의 @Builder.Default 기본값을 덮어쓰지 않도록.
            // 이게 꺼져있으면 register() 시 프론트가 안 보낸 필드(예: regDate)가 그대로 null로 저장됨.
            .setSkipNullEnabled(true);

    return modelMapper;
  }
}
