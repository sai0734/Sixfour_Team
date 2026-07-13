package com.wedding.contract.service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedding.contract.dto.ContractClauseFlagDTO;
import com.wedding.contract.dto.ContractReviewResultDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class ContractReviewServiceImpl implements ContractReviewService {

    @Value("${anthropic.api.key:}")
    private String apiKey;

    @Value("${anthropic.api.model:claude-sonnet-5}")
    private String model;

    private final ObjectMapper objectMapper;

    // 웨딩 업종(웨딩홀/스튜디오/드레스/메이크업 등) 계약서에 특화된 독소조항 검토 프롬프트.
    // 반드시 JSON만 응답하게 강하게 못박아둠 - 안 그러면 파싱이 깨짐
    private static final String PROMPT = """
      너는 대한민국 웨딩 산업(웨딩홀/스튜디오/드레스/메이크업 등) 계약서를 검토하는 소비자 보호 전문가야.
      첨부된 문서를 읽고, 이 문서가 실제로 계약서/계약 관련 문서가 맞는지 먼저 판단해.
      계약서가 맞다면, 소비자(예비 신랑/신부)에게 불리할 수 있는 독소조항을 찾아서 분석해줘.

      특히 이런 항목을 중점적으로 봐:
      - 위약금/취소수수료 조항 (과도하게 높거나, 일방적인 경우)
      - 취소/환불 규정 (환불 불가 조건이 지나치게 넓은 경우)
      - 인원 변경/축소 시 불이익 조항
      - 추가 옵션/비용을 사실상 강제하는 조항
      - 계약 해지 시 불공정한 조건
      - 손해배상 범위가 소비자에게만 불리하게 설정된 경우
      - 애매하거나 업체 재량으로만 해석 가능한 조항

      반드시 아래 JSON 형식으로만 응답하고, 그 외 설명(코드블록 표시 포함)은 절대 붙이지 마:

      {
        "looksLikeContract": true 또는 false,
        "overallRisk": "HIGH", "MEDIUM", "LOW", "SAFE" 중 하나,
        "summary": "한두 문장 총평",
        "flags": [
          {
            "clauseText": "문제가 된 조항 원문 또는 요약(1~2문장)",
            "riskLevel": "HIGH", "MEDIUM", "LOW" 중 하나,
            "reason": "왜 소비자에게 불리한지",
            "suggestion": "계약 전에 이렇게 확인/수정 요청하라는 제안"
          }
        ]
      }

      계약서가 아닌 것 같으면 looksLikeContract를 false로 하고 flags는 빈 배열로 둬.
      """;

    @Override
    public ContractReviewResultDTO review(MultipartFile file) {

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("AI 검토 기능이 아직 설정되지 않았습니다. 관리자에게 문의해주세요.");
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일을 선택해주세요.");
        }

        String contentType = file.getContentType();
        boolean isPdf = "application/pdf".equals(contentType);
        boolean isImage = contentType != null && contentType.startsWith("image/");

        if (!isPdf && !isImage) {
            throw new IllegalArgumentException("PDF 또는 이미지(JPG, PNG) 파일만 업로드할 수 있습니다.");
        }

        try {
            String base64 = Base64.getEncoder().encodeToString(file.getBytes());

            Map<String, Object> fileBlock = isPdf
                    ? Map.of(
                    "type", "document",
                    "source", Map.of("type", "base64", "media_type", "application/pdf", "data", base64)
            )
                    : Map.of(
                    "type", "image",
                    "source", Map.of("type", "base64", "media_type", contentType, "data", base64)
            );

            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "max_tokens", 4000,
                    "messages", List.of(
                            Map.of(
                                    "role", "user",
                                    "content", List.of(
                                            fileBlock,
                                            Map.of("type", "text", "text", PROMPT)
                                    )
                            )
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", "2023-06-01");
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://api.anthropic.com/v1/messages", entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            String rawText = root.path("content").get(0).path("text").asText();

            // Claude가 혹시 ```json ... ``` 코드블록으로 감싸서 줄 경우를 방어적으로 제거
            String cleaned = rawText.replace("```json", "").replace("```", "").trim();

            JsonNode resultNode = objectMapper.readTree(cleaned);

            List<ContractClauseFlagDTO> flags = new ArrayList<>();
            for (JsonNode flagNode : resultNode.path("flags")) {
                flags.add(ContractClauseFlagDTO.builder()
                        .clauseText(flagNode.path("clauseText").asText(""))
                        .riskLevel(flagNode.path("riskLevel").asText(""))
                        .reason(flagNode.path("reason").asText(""))
                        .suggestion(flagNode.path("suggestion").asText(""))
                        .build());
            }

            log.info("contract review done. overallRisk=" + resultNode.path("overallRisk").asText());

            return ContractReviewResultDTO.builder()
                    .looksLikeContract(resultNode.path("looksLikeContract").asBoolean(true))
                    .overallRisk(resultNode.path("overallRisk").asText("MEDIUM"))
                    .summary(resultNode.path("summary").asText(""))
                    .flags(flags)
                    .build();

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("계약서 AI 검토 중 오류", e);
            throw new IllegalStateException("계약서 분석 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

}