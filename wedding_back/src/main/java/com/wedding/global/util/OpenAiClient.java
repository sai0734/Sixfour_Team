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

}
