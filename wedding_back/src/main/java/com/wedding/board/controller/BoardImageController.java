package com.wedding.board.controller;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.wedding.board.dto.BoardImageDTO;
import com.wedding.board.service.BoardImageService;
import com.wedding.global.util.CustomFileUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/boardimages")
public class BoardImageController {

    private final BoardImageService service;

    private final CustomFileUtil fileUtil;

    @PostMapping("/{boardId}")
    public List<BoardImageDTO> upload(
            @PathVariable(name = "boardId") Long boardId,
            @RequestParam(name = "files") List<MultipartFile> files) {

        return service.upload(boardId, files);
    }

    @GetMapping("/board/{boardId}")
    public List<BoardImageDTO> listByBoard(@PathVariable(name = "boardId") Long boardId) {

        return service.listByBoard(boardId);
    }

    @GetMapping("/view/{fileName}")
    public ResponseEntity<Resource> viewFile(@PathVariable(name = "fileName") String fileName) {

        return fileUtil.getFile(fileName);
    }

}
