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

@Component
@RequiredArgsConstructor
public class OpenAiClient {

    private final RestTemplate restTemplate;

    @Value("${openai.api-url}")
    private String apiUrl;
    @Value("${openai.model}")
    private String model;

    public OpenAiResponseDTO getChatCompletions(List<OpenAiMessageDTO> messages, List<OpenAiToolDTO> tools) {

        OpenAiRequestDTO request = tools == null
                ? new OpenAiRequestDTO(model, messages)
                : new OpenAiRequestDTO(model, messages, tools);

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
    // 수정끝

}