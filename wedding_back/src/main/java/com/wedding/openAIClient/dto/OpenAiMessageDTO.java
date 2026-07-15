package com.wedding.openAIClient.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenAiMessageDTO {

    private String role;

    private String content;

    // 모델이 함수 호출을 요청할 때 응답에 실려오는 필드 (role="assistant"일 때만 존재)
    @JsonProperty("tool_calls")
    private List<ToolCallDTO> toolCalls;

    // 우리가 함수 실행 결과를 role="tool" 메시지로 돌려줄 때, 어떤 호출에 대한 결과인지 식별
    @JsonProperty("tool_call_id")
    private String toolCallId;

    // system/user/assistant처럼 role+content만 있는 일반 메시지를 간단히 만들 때 사용
    public static OpenAiMessageDTO of(String role, String content) {
        return OpenAiMessageDTO.builder().role(role).content(content).build();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolCallDTO {

        private String id;

        private String type; // "function"

        private FunctionCallDTO function;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FunctionCallDTO {

        private String name;

        // 모델이 JSON 문자열 형태 그대로 줌 (예: "{\"category\":\"미니\",\"maxPrice\":250000}")
        private String arguments;
    }

}