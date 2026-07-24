package com.wedding.openAIClient.service;

import com.wedding.openAIClient.dto.ChatResponseDTO;
import org.springframework.web.multipart.MultipartFile;

public interface ChatService {

    // 회원 질문에 대한 답변 생성 (Function Calling 기반)
    ChatResponseDTO getAnswer(String memberEmail, String question, String intent, String lockedCategory);

    // 드레스 사진 업로드 → 유사 아이템 추천
    ChatResponseDTO recommendSimilarDresses(String memberEmail, MultipartFile photo);

}
