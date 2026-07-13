package com.wedding.global.util;

import com.wedding.openAIClient.dto.OpenAiMessageDTO;
import com.wedding.openAIClient.dto.OpenAiRequestDTO;
import com.wedding.openAIClient.dto.OpenAiResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OpenAiClient {

    private final RestTemplate restTemplate;

    @Value("${openai.api-url}")
    private String apiUrl;
    @Value("${openai.model}")
    private String model;

    public OpenAiResponseDTO getChatCompletions(String prompt) {

        OpenAiRequestDTO request = getOpenAiRequestDTO(prompt);

        ResponseEntity<OpenAiResponseDTO> chatResponse = restTemplate.postForEntity(
                apiUrl,
                request,
                OpenAiResponseDTO.class
        );

        if(!chatResponse.getStatusCode().is2xxSuccessful() || chatResponse.getBody() == null) {
            throw new RuntimeException("OpenAI API 호출 실패");
        }

        return chatResponse.getBody();

    }

    private OpenAiRequestDTO getOpenAiRequestDTO(String prompt) {

        OpenAiMessageDTO systemMessage = new OpenAiMessageDTO("system", "당신은 웨딩 준비 전문 AI 비서입니다. 당신의 목적은 오직 결혼 준비 관련 질문에 답변하는 것입니다. 사용자가 결혼과 관련 없는 질문(예: 음식 추천, 일상 대화 등)을 하면, 반드시 '결혼 준비와 관련된 질문만 답변해 드릴 수 있습니다.'라고 정중하게 거절하세요. 다른 대답은 하지 마세요.");

        OpenAiMessageDTO userMessage = new OpenAiMessageDTO("user", prompt);

        List<OpenAiMessageDTO> messages = List.of(systemMessage, userMessage);

        return new OpenAiRequestDTO(model, messages);

    }
}
