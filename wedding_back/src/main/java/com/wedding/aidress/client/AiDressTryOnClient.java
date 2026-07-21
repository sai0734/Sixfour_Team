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

  @Value("${catvton.api.url}")
  private String catvtonUrl;

  @Value("${catvton.cloth.type:overall}")
  private String clothType;

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
    File humanFile = resolveLocalImageFile(humanImgUrl);
    File garmentFile = resolveLocalImageFile(garmentImgUrl);


    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
   body.add("person", new FileSystemResource(humanFile));
   body.add("cloth", new FileSystemResource(garmentFile));
   body.add("cloth_type", clothType);

    HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body);

    try {
      ResponseEntity<String> response =
          openAiMultipartRestTemplate.postForEntity(catvtonUrl, request, String.class);

      if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
        throw new IllegalStateException("CatVTON 호출 실패: " + response.getStatusCode());
      }

      try {
        return parseResultImageUrl(response.getBody());
      } catch (Exception e) {
        throw new IllegalStateException("CatVTON 응답 파싱 실패", e);
      }
    } catch (HttpClientErrorException e) {
     String detail = e.getResponseBodyAsString();
    log.error("CatVTON error: status={}, body={}", e.getStatusCode(), detail);
      throw new IllegalStateException(
          "CatVTON 호출 실패(" + e.getStatusCode().value() + "): " + detail, e);
    }
  }

  private String parseResultImageUrl(String responseBody) throws Exception {
    JsonNode root = objectMapper.readTree(responseBody);

    JsonNode b64Node = root.path("result_base64");
    if (!b64Node.isMissingNode() && !b64Node.asText().isBlank()) {
      return saveBase64Image(b64Node.asText());
    }

    JsonNode urlNode = root.path("result_url");
    if (!urlNode.isMissingNode() && !urlNode.asText().isBlank()) {
      return urlNode.asText();
    }
    throw new IllegalStateException(
            "CatVTON 응답에 result_base64/result_url이 없습니다: " + responseBody);
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
