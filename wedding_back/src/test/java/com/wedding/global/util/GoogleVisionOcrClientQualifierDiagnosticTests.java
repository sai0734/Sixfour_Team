package com.wedding.global.util;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import lombok.extern.log4j.Log4j2;

import static org.junit.jupiter.api.Assertions.assertTrue;

// 진단용 임시 테스트 - GoogleVisionOcrClient가 @RequiredArgsConstructor + 필드에 붙인
// @Qualifier("visionRestTemplate")로 실제로 올바른(인터셉터 없는) RestTemplate을 주입받는지,
// 아니면 @Primary가 붙은 OpenAI용 RestTemplate(Authorization 헤더 인터셉터 있음)을
// 잘못 주입받는지 직접 확인한다. Lombok이 필드의 @Qualifier를 생성자 파라미터로 못 옮긴다는
// 주장이 있어서, 실제로 어떤 RestTemplate이 주입됐는지 인터셉터 유무로 판별한다.
@SpringBootTest
@Log4j2
public class GoogleVisionOcrClientQualifierDiagnosticTests {

    @Autowired
    private GoogleVisionOcrClient googleVisionOcrClient;

    @Test
    public void testInjectedRestTemplateHasNoAuthInterceptor() {

        RestTemplate injected = (RestTemplate) ReflectionTestUtils.getField(googleVisionOcrClient, "restTemplate");

        int interceptorCount = injected.getInterceptors().size();
        log.info("GoogleVisionOcrClient에 실제로 주입된 RestTemplate의 인터셉터 개수={}", interceptorCount);
        log.info("인터셉터 목록: {}", injected.getInterceptors());

        assertTrue(interceptorCount == 0,
                "visionRestTemplate이 제대로 주입됐다면 인터셉터가 0개여야 함(OpenAI Authorization 헤더 "
                        + "인터셉터가 없어야 함). 0이 아니면 @Primary RestTemplate이 잘못 주입된 것.");
    }
}
