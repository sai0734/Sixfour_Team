package com.wedding.checkout.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Component
@Log4j2
public class TossPaymentClient {

    @Value("${toss.secret-key}")
    private String secretKey;

    @Value("${toss.confirm-url}")
    private String confirmUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 토스페이먼츠 결제 승인 API 호출 (서버-서버 통신, 시크릿 키 사용)
    public JsonNode confirmPayment(String paymentKey, String orderNumber, int amount) {

        String encodedAuth = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + encodedAuth);

        Map<String, Object> body = Map.of(
                "paymentKey", paymentKey,
                "orderId", orderNumber,
                "amount", amount
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(confirmUrl, request, String.class);
            return objectMapper.readTree(response.getBody());
        } catch (HttpClientErrorException e) {
            log.error("토스페이먼츠 승인 실패: " + e.getResponseBodyAsString());
            throw new IllegalStateException("결제 승인에 실패했습니다: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("토스페이먼츠 승인 처리 중 오류", e);
            throw new IllegalStateException("결제 승인 처리 중 오류가 발생했습니다.");
        }
    }

}