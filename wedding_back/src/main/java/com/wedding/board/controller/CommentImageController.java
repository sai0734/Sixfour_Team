package com.wedding.board.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.wedding.board.dto.CommentImageDTO;
import com.wedding.board.service.CommentImageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/commentimages")
public class CommentImageController {

    private final CommentImageService service;

    @PostMapping("/{commentId}")
    public List<CommentImageDTO> upload(
            @PathVariable(name = "commentId") Long commentId,
            @RequestParam(name = "files") List<MultipartFile> files) {

        return service.upload(commentId, files);
    }

    @GetMapping("/comment/{commentId}")
    public List<CommentImageDTO> listByComment(@PathVariable(name = "commentId") Long commentId) {

        return service.listByComment(commentId);
    }

    // 실제 파일 서빙은 /api/boardimages/view/{fileName}를 그대로 재사용
    // (같은 업로드 폴더를 쓰는 CustomFileUtil 기반이라 중복 만들 필요 없음)

}
