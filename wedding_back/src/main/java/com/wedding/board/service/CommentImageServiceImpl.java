package com.wedding.board.service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.wedding.board.domain.CommentImage;
import com.wedding.board.dto.CommentImageDTO;
import com.wedding.board.repository.CommentImageRepository;
import com.wedding.global.util.CustomFileUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class CommentImageServiceImpl implements CommentImageService {

    private final CommentImageRepository commentImageRepository;

    private final CustomFileUtil fileUtil;

    @Override
    public List<CommentImageDTO> upload(Long commentId, List<MultipartFile> files) {

        log.info("commentImage upload......... commentId=" + commentId);

        List<String> savedNames = fileUtil.saveFiles(files);

        if (savedNames == null) {
            return List.of();
        }

        int startOrder = commentImageRepository.findByCommentIdOrderBySortOrderAsc(commentId).size();

        List<CommentImage> images = IntStream.range(0, savedNames.size())
                .mapToObj(i -> CommentImage.builder()
                        .commentId(commentId)
                        .imageUrl(savedNames.get(i))
                        .sortOrder(startOrder + i)
                        .build())
                .collect(Collectors.toList());

        List<CommentImage> saved = commentImageRepository.saveAll(images);

        return saved.stream()
                .map(img -> CommentImageDTO.builder()
                        .imageId(img.getImageId())
                        .commentId(img.getCommentId())
                        .imageUrl(img.getImageUrl())
                        .sortOrder(img.getSortOrder())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<CommentImageDTO> listByComment(Long commentId) {

        List<CommentImage> result = commentImageRepository.findByCommentIdOrderBySortOrderAsc(commentId);

        return result.stream()
                .map(img -> CommentImageDTO.builder()
                        .imageId(img.getImageId())
                        .commentId(img.getCommentId())
                        .imageUrl(img.getImageUrl())
                        .sortOrder(img.getSortOrder())
                        .build())
                .collect(Collectors.toList());
    }

}
