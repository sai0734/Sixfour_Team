package com.wedding.openAIClient.controller;

import com.wedding.openAIClient.dto.ChatRequestDTO;
import com.wedding.openAIClient.dto.ChatResponseDTO;
import com.wedding.openAIClient.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

        ChatResponseDTO response = chatService.getAnswer(principal.getName(), chatRequestDTO.getMessage());

        return ResponseEntity.ok(response);
    }
}