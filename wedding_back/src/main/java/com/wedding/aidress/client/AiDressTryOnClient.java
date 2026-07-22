package com.wedding.aidress.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
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
 * CatVTON 합성. 사람 사진·합성 결과 모두 upload에 저장하지 않고 base64만 반환한다.
 * 드레스 원본만 upload에서 읽는다.
 */
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

  public AiDressTryOnClient(
      @Qualifier("openAiMultipartRestTemplate") RestTemplate openAiMultipartRestTemplate,
      ObjectMapper objectMapper) {
    this.openAiMultipartRestTemplate = openAiMultipartRestTemplate;
    this.objectMapper = objectMapper;
  }

  /**
   * @param personBytes 브라우저에서 받은 사람 사진 (디스크 저장 없음)
   * @param personFilename multipart 파일명용
   * @param garmentImgUrl 드레스 이미지 (upload 경로)
   * @return PNG base64 (data: 접두사 없음)
   */
  public String synthesize(byte[] personBytes, String personFilename, String garmentImgUrl) {
    if (personBytes == null || personBytes.length == 0) {
      throw new IllegalArgumentException("합성할 내 사진이 없습니다.");
    }

    String safeName =
        StringUtils.hasText(personFilename) ? personFilename : "person.jpg";
    ByteArrayResource personResource =
        new ByteArrayResource(personBytes) {
          @Override
          public String getFilename() {
            return safeName;
          }
        };

    File garmentFile = resolveLocalImageFile(garmentImgUrl);

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("person", personResource);
    body.add("cloth", new FileSystemResource(garmentFile));
    body.add("cloth_type", clothType);

    try {
      ResponseEntity<String> response =
          openAiMultipartRestTemplate.postForEntity(
              catvtonUrl, new HttpEntity<>(body), String.class);

      if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
        throw new IllegalStateException("CatVTON 호출 실패: " + response.getStatusCode());
      }
      return parseResultBase64(response.getBody());
    } catch (HttpClientErrorException e) {
      String detail = e.getResponseBodyAsString();
      log.error("CatVTON error: status={}, body={}", e.getStatusCode(), detail);
      throw new IllegalStateException(
          "CatVTON 호출 실패(" + e.getStatusCode().value() + "): " + detail, e);
    } catch (IllegalStateException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException("CatVTON 응답 파싱 실패", e);
    }
  }

  private String parseResultBase64(String responseBody) throws Exception {
    JsonNode root = objectMapper.readTree(responseBody);
    JsonNode b64Node = root.path("result_base64");
    if (!b64Node.isMissingNode() && !b64Node.asText().isBlank()) {
      return b64Node.asText().trim();
    }
    throw new IllegalStateException("CatVTON 응답에 result_base64가 없습니다: " + responseBody);
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
