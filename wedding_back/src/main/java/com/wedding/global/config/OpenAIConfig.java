package com.wedding.global.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@Configuration
public class OpenAIConfig {

  @Value("${openai.api-key:}")
  private String apiKey;

  @Bean
  @Primary
  public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
    return restTemplateBuilder
        .additionalInterceptors(
            (request, body, execution) -> {
              request.getHeaders().add("Authorization", "Bearer " + apiKey);
              return execution.execute(request, body);
            })
        .build();
  }

  /** CatVTON multipart — Auth 없음, 합성 대기 시간 여유 */
  @Bean
  public RestTemplate openAiMultipartRestTemplate(RestTemplateBuilder restTemplateBuilder) {
    return restTemplateBuilder
        .setConnectTimeout(Duration.ofSeconds(30))
        .setReadTimeout(Duration.ofMinutes(3))
        .build();
  }

  /** OpenAI Images Edit — Auth + 긴 타임아웃 */
  @Bean
  public RestTemplate openAiImageRestTemplate(RestTemplateBuilder restTemplateBuilder) {
    return restTemplateBuilder
        .setConnectTimeout(Duration.ofSeconds(30))
        .setReadTimeout(Duration.ofMinutes(3))
        .additionalInterceptors(
            (request, body, execution) -> {
              request.getHeaders().setBearerAuth(apiKey == null ? "" : apiKey);
              return execution.execute(request, body);
            })
        .build();
  }

    /** Google Vision OCR 전용 - Auth 헤더 인터셉터 없음(쿼리파라미터 ?key=로 별도 인증하므로 불필요) */
    @Bean
    public RestTemplate visionRestTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.build();
    }
}
