package com.wedding.openAIClient.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OpenAiRequestDTO {

    private String model;

    private List<OpenAiMessageDTO> messages;

    public OpenAiRequestDTO(String model,List<OpenAiMessageDTO> messages) {
        this.model = model;
        this.messages = messages;
    }

}
