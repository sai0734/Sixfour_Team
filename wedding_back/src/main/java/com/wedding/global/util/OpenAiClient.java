package com.wedding.global.util;

import com.wedding.openAIClient.dto.OpenAiRequestDTO;
import com.wedding.openAIClient.dto.OpenAiResponseDTO;
import com.wedding.openAIClient.dto.OpenAiToolDTO;
import com.wedding.openAIClient.dto.OpenAiMessageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenAiClient {

    private final RestTemplate restTemplate;

    @Value("${openai.api-url}")
    private String apiUrl;
    @Value("${openai.model}")
    private String model;

    public OpenAiResponseDTO getChatCompletions(List<OpenAiMessageDTO> messages, List<OpenAiToolDTO> tools) {
        return getChatCompletions(messages, tools, null);
    }

    // toolChoice="required"를 넘기면 tools 중 하나를 반드시 호출해야 함 - 모델이 아무 함수도
    // 호출하지 않고 바로 텍스트로만 답해버리는(그래서 tool_choice="required"를 만족 못 시키는) 경우를 막고 싶을 때 사용
    public OpenAiResponseDTO getChatCompletions(List<OpenAiMessageDTO> messages, List<OpenAiToolDTO> tools, String toolChoice) {

        OpenAiRequestDTO request = tools == null
                ? new OpenAiRequestDTO(model, messages)
                : new OpenAiRequestDTO(model, messages, tools, toolChoice);

        ResponseEntity<OpenAiResponseDTO> chatResponse = restTemplate.postForEntity(
                apiUrl,
                request,
                OpenAiResponseDTO.class
        );

        if (!chatResponse.getStatusCode().is2xxSuccessful() || chatResponse.getBody() == null) {
            throw new RuntimeException("OpenAI API 호출 실패");
        }

        return chatResponse.getBody();
    }

    // AI 웨딩플랜 자세히 모드(5단계) - 순수 JSON 텍스트만 응답하게 강제 (response_format: json_object).
    // 툴 호출은 안 쓰므로 tools 없이 감.
    public OpenAiResponseDTO getJsonChatCompletion(List<OpenAiMessageDTO> messages) {

        OpenAiRequestDTO request = new OpenAiRequestDTO(model, messages, Map.of("type", "json_object"));

        ResponseEntity<OpenAiResponseDTO> chatResponse = restTemplate.postForEntity(
                apiUrl,
                request,
                OpenAiResponseDTO.class
        );

        if (!chatResponse.getStatusCode().is2xxSuccessful() || chatResponse.getBody() == null) {
            throw new RuntimeException("OpenAI API 호출 실패");
        }

        return chatResponse.getBody();
    }

    // Vision 지원 모델에게 이미지+프롬프트를 보내 텍스트 설명만 받음. OpenAiMessageDTO.content는
    // 순수 문자열이라 멀티모달 content를 못 담아서, 이 호출만 요청 바디를 직접 구성한다.
    public String describeImage(byte[] imageBytes, String contentType, String prompt) {

        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String mimeType = (contentType != null && !contentType.isBlank()) ? contentType : "image/jpeg";
        String dataUrl = "data:" + mimeType + ";base64," + base64Image;

        Map<String, Object> textPart = Map.of("type", "text", "text", prompt);
        Map<String, Object> imagePart = Map.of(
                "type", "image_url",
                "image_url", Map.of("url", dataUrl));

        Map<String, Object> userMessage = Map.of(
                "role", "user",
                "content", List.of(textPart, imagePart));

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(userMessage));

        ResponseEntity<OpenAiResponseDTO> response = restTemplate.postForEntity(
                apiUrl,
                requestBody,
                OpenAiResponseDTO.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("OpenAI 이미지 분석 호출 실패");
        }

        return response.getBody().getChoices().getFirst().getMessage().getContent();
    }

    // Vision 입력 + JSON 강제 응답을 합친 버전. exampleTurns는 실제 사진 없이 텍스트로만 된
    // few-shot 예시(사용자 발화 → 모범 답안)를 끼워 넣을 수 있게 한 것 - 구조화 추출 작업은
    // 예시 하나만 보여줘도 정확도가 크게 오르는 경우가 많다. 예시가 필요 없으면 List.of()를 넘기면 됨.
    // detail:"high"는 표/영수증처럼 글자가 빽빽한 문서를 읽을 때 정확도를 올려주는 옵션이라 항상 켜둔다.
    public String describeImageAsJson(byte[] imageBytes, String contentType, String systemPrompt,
                                      List<OpenAiMessageDTO> exampleTurns, String userPrompt) {

        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String mimeType = (contentType != null && !contentType.isBlank()) ? contentType : "image/jpeg";
        String dataUrl = "data:" + mimeType + ";base64," + base64Image;

        Map<String, Object> textPart = Map.of("type", "text", "text", userPrompt);
        Map<String, Object> imagePart = Map.of(
                "type", "image_url",
                "image_url", Map.of("url", dataUrl, "detail", "high"));

        Map<String, Object> systemMessage = Map.of("role", "system", "content", systemPrompt);
        Map<String, Object> userMessage = Map.of(
                "role", "user",
                "content", List.of(textPart, imagePart));

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(systemMessage);
        for (OpenAiMessageDTO turn : exampleTurns) {
            messages.add(Map.of("role", turn.getRole(), "content", turn.getContent()));
        }
        messages.add(userMessage);

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", messages,
                "response_format", Map.of("type", "json_object"));

        ResponseEntity<OpenAiResponseDTO> response = restTemplate.postForEntity(
                apiUrl,
                requestBody,
                OpenAiResponseDTO.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("OpenAI 견적서 분석 호출 실패");
        }

        return response.getBody().getChoices().getFirst().getMessage().getContent();
    }

}
