package com.wedding.aidress.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * CatVTON 합성 결과에 사용자 프롬프트 기반 배경을 입힌다 (OpenAI Images Edit).
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

  @Value("${com.wedding.upload.path}")
  private String uploadPath;

  @Value("${com.wedding.server.host}")
  private String serverHost;

  public OpenAiBackgroundClient(
      @Qualifier("openAiImageRestTemplate") RestTemplate openAiImageRestTemplate,
      ObjectMapper objectMapper) {
    this.openAiImageRestTemplate = openAiImageRestTemplate;
    this.objectMapper = objectMapper;
  }

  public boolean isConfigured() {
    return StringUtils.hasText(apiKey);
  }

  public String applyBackground(String tryOnImageUrl, String backgroundPrompt) {
    if (!StringUtils.hasText(backgroundPrompt)) {
      return tryOnImageUrl;
    }
    if (!isConfigured()) {
      throw new IllegalStateException(
          "배경 프롬프트를 쓰려면 OPENAI_API_KEY(또는 openai.api-key)가 필요합니다.");
    }

    File source = resolveLocalImageFile(tryOnImageUrl);
    String prompt =
        "Keep the person and their clothing/wedding dress exactly as they appear in the photo. "
            + "Do not change face, body, pose, or garment details. "
            + "Replace only the background with this scene: "
            + backgroundPrompt.trim()
            + ". Photorealistic wedding photography, natural lighting.";

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("model", imageModel);
    body.add("prompt", prompt);
    body.add("image", new FileSystemResource(source));
    body.add("size", imageSize);
    body.add("quality", "high");

    try {
      ResponseEntity<String> response =
          openAiImageRestTemplate.postForEntity(editUrl, new HttpEntity<>(body), String.class);

      if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
        throw new IllegalStateException("OpenAI 배경 합성 실패: " + response.getStatusCode());
      }
      return parseAndSave(response.getBody());
    } catch (HttpClientErrorException e) {
      log.error("OpenAI background error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
      throw new IllegalStateException(
          "OpenAI 배경 합성 실패(" + e.getStatusCode().value() + "): " + e.getResponseBodyAsString(), e);
    } catch (IllegalStateException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException("OpenAI 배경 응답 처리 실패", e);
    }
  }

  private String parseAndSave(String responseBody) throws Exception {
    JsonNode root = objectMapper.readTree(responseBody);
    JsonNode data0 = root.path("data").path(0);
    if (data0.isMissingNode()) {
      throw new IllegalStateException("OpenAI 응답에 data[0]이 없습니다: " + responseBody);
    }

    JsonNode b64 = data0.path("b64_json");
    if (!b64.isMissingNode() && StringUtils.hasText(b64.asText())) {
      return saveBase64Image(b64.asText());
    }

    JsonNode url = data0.path("url");
    if (!url.isMissingNode() && StringUtils.hasText(url.asText())) {
      return url.asText();
    }
    throw new IllegalStateException("OpenAI 응답에 b64_json/url이 없습니다: " + responseBody);
  }

  private String saveBase64Image(String base64) throws Exception {
    Path uploadDir = Paths.get(uploadPath);
    if (!Files.exists(uploadDir)) {
      Files.createDirectories(uploadDir);
    }
    String fileName = "aidress_bg_" + UUID.randomUUID() + ".png";
    Files.write(uploadDir.resolve(fileName), Base64.getDecoder().decode(base64));
    return serverHost + "/api/companies/images/view/" + fileName;
  }

  private File resolveLocalImageFile(String imageUrl) {
    String fileName = imageUrl;
    int viewIdx = imageUrl.indexOf("/view/");
    if (viewIdx >= 0) {
      fileName = imageUrl.substring(viewIdx + "/view/".length());
    } else if (imageUrl.contains("/")) {
      fileName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
    }
    File file = Paths.get(uploadPath, fileName).toFile();
    if (!file.exists() || !file.canRead()) {
      throw new IllegalArgumentException("배경 합성용 이미지를 찾을 수 없습니다: " + fileName);
    }
    return file;
  }
}
