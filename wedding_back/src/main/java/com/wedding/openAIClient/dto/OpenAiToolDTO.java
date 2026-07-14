package com.wedding.openAIClient.dto;

import lombok.*;

import java.util.Map;

// OpenAI에게 "이런 함수를 쓸 수 있다"고 알려주는 tools 스펙 (Function Calling용)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OpenAiToolDTO {

    @Builder.Default
    private String type = "function";

    private FunctionSpec function;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class FunctionSpec {

        private String name;

        private String description;

        // JSON Schema 형태 그대로 (type/properties/required 등) - Map으로 두면 자유롭게 구성 가능
        private Map<String, Object> parameters;

    }
}
