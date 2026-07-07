package com.wedding.openAIClient.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatRequestDTO {

    private String message;

    public ChatRequestDTO(String message) {
        this.message = message;
    }
}
