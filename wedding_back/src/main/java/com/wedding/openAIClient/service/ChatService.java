package com.wedding.openAIClient.service;

import com.wedding.global.util.OpenAiClient;
import com.wedding.openAIClient.dto.OpenAiResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final OpenAiClient openAiClient;

    public String getAnswer(String question) {

        // OpenAiClient를 통해 OpenAi에 질문 전달
        OpenAiResponseDTO openAiResponseDTO = openAiClient.getChatCompletions(question);

        // 응답 중 첫 번째 메세지의 content 추출
        return  openAiResponseDTO.getChoices().getFirst().getMessage().getContent();

    }

}
