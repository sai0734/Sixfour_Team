package com.wedding.openAIClient.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ChatResponseDTO {

    private String answer;
    private List<ChatReferenceDTO> references;

    public ChatResponseDTO(String answer, List<ChatReferenceDTO> references) {
        this.answer = answer;
        this.references = references;
    }

    public static ChatResponseDTO of(String answer, List<ChatReferenceDTO> references) {
        return new ChatResponseDTO(answer, references);
    }
}