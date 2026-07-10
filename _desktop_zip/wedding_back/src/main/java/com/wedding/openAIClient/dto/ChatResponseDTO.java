package com.wedding.openAIClient.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatResponseDTO {

    private String answer;

    public ChatResponseDTO(String answer) {
        this.answer = answer;
    }

    public static ChatResponseDTO of(String answer) {
        return new ChatResponseDTO(answer);
    }
}
