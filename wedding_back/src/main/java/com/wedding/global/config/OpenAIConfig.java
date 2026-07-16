package com.wedding.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@Configuration
public class OpenAIConfig {

    // 키가 비어있어도(로컬에 아직 발급 안 된 상태) 서버 기동은 되게 기본값(빈 문자열)을 줌.
    // 실제 호출 시점에는 여전히 인증 실패로 막히지만, 최소한 앱 전체가 죽는 건 방지함.
    @Value("${openai.api-key:}")
    private String apiKey;

    @Bean
    @Primary
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder
                .additionalInterceptors(((request, body, execution) -> {

                    request.getHeaders().add("Authorization", "Bearer " + apiKey);

                    return execution.execute(request, body);
                }))
                .build();
    }

    /** multipart 이미지 편집용 — Content-Type boundary는 RestTemplate이 자동 설정 */
    @Bean
    public RestTemplate openAiMultipartRestTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.build();
    }

}