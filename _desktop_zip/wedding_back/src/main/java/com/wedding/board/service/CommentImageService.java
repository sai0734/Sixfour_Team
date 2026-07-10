package com.wedding.board.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.wedding.board.dto.CommentImageDTO;

public interface CommentImageService {

    List<CommentImageDTO> upload(Long commentId, List<MultipartFile> files);

    List<CommentImageDTO> listByComment(Long commentId);

    void remove(Long imageId);

}
