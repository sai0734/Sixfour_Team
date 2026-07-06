package com.wedding.board.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.wedding.board.dto.BoardImageDTO;

public interface BoardImageService {

    List<BoardImageDTO> upload(Long boardId, List<MultipartFile> files);

    List<BoardImageDTO> listByBoard(Long boardId);

    void remove(Long imageId);

}
