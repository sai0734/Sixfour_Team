package com.wedding.openAIClient.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OpenAiResponseDTO {

    // GPT가 생성한 응답 선택지 리스트
    private List<Choice> choices;

    @Getter
    public static class Choice {
        // 생성된 메세지 정보
        private OpenAiMessageDTO message;
    }
}