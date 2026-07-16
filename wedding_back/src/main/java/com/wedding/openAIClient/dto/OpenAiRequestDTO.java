package com.wedding.openAIClient.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OpenAiRequestDTO {

    private String model;

    private List<OpenAiMessageDTO> messages;

    private List<OpenAiToolDTO> tools;

    // "auto"(기본), "required"(반드시 tools 중 하나 호출), "none" 중 하나. 안 쓰면 null(=auto와 동일)
    @JsonProperty("tool_choice")
    private String toolChoice;

    // JSON 강제 응답용 (예: {"type": "json_object"}) - AI 웨딩플랜 자세히 모드(5단계)에서 사용
    @JsonProperty("response_format")
    private Map<String, String> responseFormat;

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

    // AI 웨딩플랜 - JSON 모드 전용 생성자 (tools 없이 response_format만 지정)
    public OpenAiRequestDTO(String model, List<OpenAiMessageDTO> messages, Map<String, String> responseFormat) {
        this.model = model;
        this.messages = messages;
        this.responseFormat = responseFormat;
    }

}
