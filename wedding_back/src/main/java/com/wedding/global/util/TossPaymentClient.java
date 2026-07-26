package com.wedding.global.util;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

    private HttpHeaders buildHeaders() {

        String encodedAuth = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + encodedAuth);
        return headers;

    }

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
            // String.class로 받으면 응답 Content-Type에 charset이 없을 때 RestTemplate이
            // ISO-8859-1로 잘못 디코딩해서 한글(결제수단 등)이 깨진 채로 파싱됨 - 실제로 이
            // 문제가 있었음. JsonNode.class로 바로 받으면 Jackson 컨버터가 바이트를 직접
            // UTF-8로 읽어서 이 문제가 없다.
            return restTemplate.postForEntity(confirmUrl, request, JsonNode.class).getBody();
        } catch (HttpClientErrorException e) {
            log.error("토스페이먼츠 승인 실패: " + e.getResponseBodyAsString());
            throw new IllegalStateException("결제 승인에 실패했습니다: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("토스페이먼츠 승인 처리 중 오류", e);
            throw new IllegalStateException("결제 승인 처리 중 오류가 발생했습니다.");
        }
    }

    // 결제 취소(환불) API 호출
    public JsonNode cancelPayment(String paymentKey, String cancelReason) {

        String cancelUrl = "https://api.tosspayments.com/v1/payments/" + paymentKey + "/cancel";

        Map<String, Object> body = Map.of("cancelReason", cancelReason);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, buildHeaders());

        try {
            return restTemplate.postForEntity(cancelUrl, request, JsonNode.class).getBody();
        } catch (HttpClientErrorException e) {
            log.error("토스페이먼츠 환불 실패: " + e.getResponseBodyAsString());
            throw new IllegalStateException("환불 처리에 실패했습니다: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("토스페이먼츠 환불 처리 중 오류", e);
            throw new IllegalStateException("환불 처리 중 오류가 발생했습니다.");
        }

    }

}