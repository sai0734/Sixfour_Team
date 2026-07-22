package com.wedding.aidress.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * 배경만 교체 (OpenAI). 결과는 upload에 저장하지 않고 base64만 반환.
 */
@Component
@Log4j2
public class OpenAiBackgroundClient {

  private final RestTemplate openAiImageRestTemplate;
  private final ObjectMapper objectMapper;

  @Value("${openai.api-key:}")
  private String apiKey;

  @Value("${openai.image.model:gpt-image-1}")
  private String imageModel;

  @Value("${openai.image.edit-url:https://api.openai.com/v1/images/edits}")
  private String editUrl;

  @Value("${openai.image.size:1024x1536}")
  private String imageSize;

  public OpenAiBackgroundClient(
      @Qualifier("openAiImageRestTemplate") RestTemplate openAiImageRestTemplate,
      ObjectMapper objectMapper) {
    this.openAiImageRestTemplate = openAiImageRestTemplate;
    this.objectMapper = objectMapper;
  }

  public boolean isConfigured() {
    return StringUtils.hasText(apiKey);
  }

  /**
   * @param tryOnImageBase64 CatVTON 결과 PNG base64
   * @return 배경 적용 PNG base64 (디스크 저장 없음)
   */
  public String applyBackgroundFromBase64(String tryOnImageBase64, String backgroundPrompt) {
    if (!StringUtils.hasText(backgroundPrompt)) {
      return tryOnImageBase64;
    }
    if (!isConfigured()) {
      throw new IllegalStateException(
          "배경 프롬프트를 쓰려면 OPENAI_API_KEY(또는 openai.api-key)가 필요합니다.");
    }
    if (!StringUtils.hasText(tryOnImageBase64)) {
      throw new IllegalArgumentException("배경 합성용 이미지가 없습니다.");
    }

    byte[] imageBytes = Base64.getDecoder().decode(stripDataUrlPrefix(tryOnImageBase64));
    ByteArrayResource imageResource =
        new ByteArrayResource(imageBytes) {
          @Override
          public String getFilename() {
            return "tryon.png";
          }
        };

    String prompt =
        "Keep the person and their wedding dress EXACTLY as they appear — "
            + "same dress design, lace, color, silhouette and fabric. "
            + "Do not redesign the dress. Replace ONLY the background with: "
            + backgroundPrompt.trim()
            + ". Photorealistic wedding photography.";

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("model", imageModel);
    body.add("prompt", prompt);
    body.add("image", imageResource);
    body.add("size", imageSize);
    body.add("quality", "high");
    // gpt-image-* 는 b64_json 기본 반환. response_format은 dall-e 전용이라내면 400 남.
    try {
      ResponseEntity<String> response =
          openAiImageRestTemplate.postForEntity(editUrl, new HttpEntity<>(body), String.class);
      if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
        throw new IllegalStateException("OpenAI 배경 합성 실패: " + response.getStatusCode());
      }
      return parseResultBase64(response.getBody());
    } catch (HttpClientErrorException e) {
      log.error(
          "OpenAI background error: status={}, body={}",
          e.getStatusCode(),
          e.getResponseBodyAsString());
      throw new IllegalStateException(
          "OpenAI 배경 합성 실패(" + e.getStatusCode().value() + "): " + e.getResponseBodyAsString(),
          e);
    } catch (IllegalStateException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException("OpenAI 배경 응답 처리 실패", e);
    }
  }

  private String parseResultBase64(String responseBody) throws Exception {
    JsonNode root = objectMapper.readTree(responseBody);
    JsonNode data0 = root.path("data").path(0);
    if (data0.isMissingNode()) {
      throw new IllegalStateException("OpenAI 응답에 data[0]이 없습니다: " + responseBody);
    }
    JsonNode b64 = data0.path("b64_json");
    if (!b64.isMissingNode() && StringUtils.hasText(b64.asText())) {
      return b64.asText().trim();
    }

    JsonNode url = data0.path("url");
    if (!url.isMissingNode() && StringUtils.hasText(url.asText())) {
      return downloadUrlAsBase64(url.asText());
    }
    throw new IllegalStateException("OpenAI 응답에 b64_json/url이 없습니다: " + responseBody);
  }

  private String downloadUrlAsBase64(String imageUrl) throws Exception {
    ResponseEntity<byte[]> response =
        openAiImageRestTemplate.getForEntity(imageUrl, byte[].class);
    if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
      throw new IllegalStateException("OpenAI 결과 이미지 다운로드 실패: " + response.getStatusCode());
    }
    return Base64.getEncoder().encodeToString(response.getBody());
  }

  private static String stripDataUrlPrefix(String value) {
    int comma = value.indexOf(',');
    if (value.startsWith("data:") && comma >= 0) {
      return value.substring(comma + 1);
    }
    return value;
  }
}
