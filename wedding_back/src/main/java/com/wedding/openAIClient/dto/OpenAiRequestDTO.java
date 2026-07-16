package com.wedding.openAIClient.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OpenAiRequestDTO {

    private String model;

    private List<OpenAiMessageDTO> messages;

    private List<OpenAiToolDTO> tools;

    // "auto"(기본), "required"(반드시 tools 중 하나 호출), "none" 중 하나. 안 쓰면 null(=auto와 동일)
    @JsonProperty("tool_choice")
    private String toolChoice;

    public OpenAiRequestDTO(String model, List<OpenAiMessageDTO> messages) {
        this.model = model;
        this.messages = messages;
    }

    public OpenAiRequestDTO(String model, List<OpenAiMessageDTO> messages, List<OpenAiToolDTO> tools) {
        this(model, messages, tools, null);
    }

    public OpenAiRequestDTO(String model, List<OpenAiMessageDTO> messages, List<OpenAiToolDTO> tools, String toolChoice) {
        this.model = model;
        this.messages = messages;
        this.tools = tools;
        this.toolChoice = toolChoice;
    }

}