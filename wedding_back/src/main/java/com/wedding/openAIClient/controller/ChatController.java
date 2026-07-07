package com.wedding.openAIClient.controller;

import com.wedding.openAIClient.dto.ChatRequestDTO;
import com.wedding.openAIClient.dto.ChatResponseDTO;
import com.wedding.openAIClient.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Log4j2
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ChatResponseDTO> chat(@RequestBody ChatRequestDTO chatRequestDTO) {
        String answer = chatService.getAnswer(chatRequestDTO.getMessage());

        log.info("ChatController_chat 실행~~~~~~~~");

        return ResponseEntity.ok(ChatResponseDTO.of(answer));
    }
}
