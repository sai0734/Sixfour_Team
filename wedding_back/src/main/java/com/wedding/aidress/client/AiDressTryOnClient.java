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
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
@Log4j2
public class AiDressTryOnClient {

  private final RestTemplate openAiMultipartRestTemplate;
  private final ObjectMapper objectMapper;

  @Value("${openai.api-key:}")
  private String key;

  @Value("${openai.edit.model}")
  private String model;

  @Value("${openai.edit.url}")
  private String editUrl;

  @Value("${com.wedding.upload.path}")
  private String uploadPath;

  @Value("${com.wedding.server.host}")
  private String serverHost;

  public AiDressTryOnClient(
      @Qualifier("openAiMultipartRestTemplate") RestTemplate openAiMultipartRestTemplate,
      ObjectMapper objectMapper) {
    this.openAiMultipartRestTemplate = openAiMultipartRestTemplate;
    this.objectMapper = objectMapper;
  }

  public String synthesize(String humanImgUrl, String garmentImgUrl) {
    if (key == null || key.isBlank()) {
      throw new IllegalStateException(
          "openai.api-key가 설정되지 않았습니다. IntelliJ Run 환경변수 OPENAI_API_KEY를 확인하세요.");
    }

    File humanFile = resolveLocalImageFile(humanImgUrl);
    File garmentFile = resolveLocalImageFile(garmentImgUrl);

    String prompt = """
        이미지 1: 그대로 유지할 사람(회원) 사진.
        이미지 2: 입힐 웨딩드레스 참조 이미지.
        이미지 1의 사람에게 이미지 2의 웨딩드레스를 입혀 주세요.
        얼굴, 이목구비, 피부톤, 체형, 손, 포즈, 헤어, 표정, 배경, 카메라 각도, 구도, 조명은
        정확히 그대로 유지하세요.
        변경하는 것은 옷(드레스)만 해당합니다.
        드레스가 자연스럽게 맞도록 주름, 가림, 그림자를 사실적으로 표현하세요.
        액세서리, 가방, 텍스트, 로고는 추가하지 마세요.
        """;

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("model", model);
    body.add("prompt", prompt);
    body.add("image[]", new FileSystemResource(humanFile));
    body.add("image[]", new FileSystemResource(garmentFile));
    body.add("size", "1024x1536");
    body.add("quality", "medium");
    body.add("output_format", "png");

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(key);

    HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

    try {
      ResponseEntity<String> response =
          openAiMultipartRestTemplate.postForEntity(editUrl, request, String.class);

      if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
        throw new IllegalStateException("OpenAI image edit 호출 실패: " + response.getStatusCode());
      }

      try {
        return parseResultImageUrl(response.getBody());
      } catch (Exception e) {
        throw new IllegalStateException("OpenAI image edit 응답 파싱 실패", e);
      }
    } catch (HttpClientErrorException.Unauthorized e) {
      throw new IllegalStateException(
          "OpenAI API 인증 실패(401). OPENAI_API_KEY가 올바른지 확인하세요.", e);
    } catch (HttpClientErrorException e) {
      String detail = e.getResponseBodyAsString();
      log.error("OpenAI image edit error: status={}, body={}", e.getStatusCode(), detail);
      throw new IllegalStateException(
          "OpenAI image edit 호출 실패(" + e.getStatusCode().value() + "): " + detail, e);
    }
  }

  private String parseResultImageUrl(String responseBody) throws Exception {
    JsonNode root = objectMapper.readTree(responseBody);
    JsonNode dataNode = root.path("data").path(0);

    JsonNode urlNode = dataNode.path("url");
    if (!urlNode.isMissingNode() && !urlNode.asText().isBlank()) {
      return urlNode.asText();
    }

    JsonNode b64Node = dataNode.path("b64_json");
    if (!b64Node.isMissingNode() && !b64Node.asText().isBlank()) {
      return saveBase64Image(b64Node.asText());
    }

    throw new IllegalStateException("OpenAI 응답에 image url/b64_json이 없습니다: " + responseBody);
  }

  private String saveBase64Image(String base64) throws Exception {
    Path uploadDir = Paths.get(uploadPath);
    if (!Files.exists(uploadDir)) {
      Files.createDirectories(uploadDir);
    }

    String fileName = "aidress_" + UUID.randomUUID() + ".png";
    Path savePath = uploadDir.resolve(fileName);
    Files.write(savePath, Base64.getDecoder().decode(base64));

    return serverHost + "/api/companies/images/view/" + fileName;
  }

  private File resolveLocalImageFile(String imageUrl) {
    String fileName = extractFileName(imageUrl);
    File file = resolveReadableFile(fileName);
    if (file != null) {
      return file;
    }
    throw new IllegalArgumentException("이미지 파일을 찾을 수 없습니다: " + fileName);
  }

  private File resolveReadableFile(String fileName) {
    Path path = Paths.get(uploadPath, fileName);
    File file = path.toFile();
    if (file.exists() && file.canRead()) {
      return file;
    }

    if (fileName.startsWith("s_")) {
      File original = Paths.get(uploadPath, fileName.substring(2)).toFile();
      if (original.exists() && original.canRead()) {
        return original;
      }
    }

    File fallback = Paths.get(uploadPath, "winter.jpg").toFile();
    if (fallback.exists() && fallback.canRead()) {
      log.warn("이미지 파일 없음({}), winter.jpg로 대체합니다.", fileName);
      return fallback;
    }

    return null;
  }

  private String extractFileName(String imageUrl) {
    if (imageUrl == null || imageUrl.isBlank()) {
      throw new IllegalArgumentException("이미지 URL이 비어 있습니다.");
    }

    int viewIdx = imageUrl.indexOf("/view/");
    if (viewIdx >= 0) {
      return imageUrl.substring(viewIdx + "/view/".length());
    }
    return imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
  }
}
