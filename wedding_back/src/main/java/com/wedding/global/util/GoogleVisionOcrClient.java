package com.wedding.global.util;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class GoogleVisionOcrClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${google.vision.api-key}")
    private String apiKey;

    @Value("${google.vision.api-url}")
    private String apiUrl;

    public GoogleVisionOcrClient(
            @Qualifier("visionRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public String extractText(byte[] imageBytes) {

        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        Map<String, Object> image = Map.of("content", base64Image);
        Map<String, Object> feature = Map.of("type", "DOCUMENT_TEXT_DETECTION");
        Map<String, Object> request = Map.of("image", image, "features", List.of(feature));
        Map<String, Object> requestBody = Map.of("requests", List.of(request));

        String url = apiUrl + "?key=" + apiKey;

        ResponseEntity<String> response = restTemplate.postForEntity(url, requestBody, String.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Google Vision OCR 호출 실패");
        }

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode responses = root.get("responses");
            if (responses == null || responses.isEmpty()) {
                return "";
            }

            JsonNode first = responses.get(0);
            if (first.has("error")) {
                throw new RuntimeException("Google Vision OCR 오류: " + first.get("error").toString());
            }

            JsonNode fullText = first.get("fullTextAnnotation");
            if (fullText == null || !fullText.hasNonNull("text")) {
                return "";
            }
            return fullText.get("text").asText();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Google Vision OCR 응답 파싱 실패", e);
        }
    }
}