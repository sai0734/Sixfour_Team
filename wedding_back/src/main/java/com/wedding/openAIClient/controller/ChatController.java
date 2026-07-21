package com.wedding.openAIClient.controller;

import com.wedding.openAIClient.dto.ChatRequestDTO;
import com.wedding.openAIClient.dto.ChatResponseDTO;
import com.wedding.openAIClient.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@RestController
@Log4j2
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // 회원별 대화 이력을 구분해야 하니 로그인 사용자만 호출 가능하게, 이메일도 넘겨야 함
    @PreAuthorize("hasAnyRole('USER')")
    @PostMapping
    public ResponseEntity<ChatResponseDTO> chat(Principal principal, @RequestBody ChatRequestDTO chatRequestDTO) {

        log.info("ChatController_chat 실행~~~~~~~~");

        ChatResponseDTO response = chatService.getAnswer(
                principal.getName(), chatRequestDTO.getMessage(), chatRequestDTO.getIntent(),
                chatRequestDTO.getCompanyCategory());

        return ResponseEntity.ok(response);
    }

    // 드레스 사진 업로드 → 비슷한 드레스 아이템 추천 (버튼 메뉴 전용, 기존 대화형 흐름과 별개)
    @PreAuthorize("hasAnyRole('USER')")
    @PostMapping(value = "/dress-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ChatResponseDTO> recommendByPhoto(
            Principal principal, @RequestPart("photo") MultipartFile photo) {

        log.info("ChatController_recommendByPhoto 실행~~~~~~~~");

        ChatResponseDTO response = chatService.recommendSimilarDresses(principal.getName(), photo);

        return ResponseEntity.ok(response);
    }
}
