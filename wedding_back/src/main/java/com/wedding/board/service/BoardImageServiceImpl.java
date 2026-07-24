package com.wedding.board.service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.wedding.board.domain.BoardImage;
import com.wedding.board.dto.BoardImageDTO;
import com.wedding.board.repository.BoardImageRepository;
import com.wedding.global.util.CustomFileUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class BoardImageServiceImpl implements BoardImageService {

    private final BoardImageRepository boardImageRepository;

    private final CustomFileUtil fileUtil;

    @Override
    public List<BoardImageDTO> upload(Long boardId, List<MultipartFile> files) {

        log.info("boardImage upload......... boardId=" + boardId);

        List<String> savedNames = fileUtil.saveFiles(files);

        if (savedNames == null) {
            return List.of();
        }

        int startOrder = boardImageRepository.findByBoardIdOrderBySortOrderAsc(boardId).size();

        List<BoardImage> images = IntStream.range(0, savedNames.size())
                .mapToObj(i -> BoardImage.builder()
                        .boardId(boardId)
                        .imageUrl(savedNames.get(i))
                        .sortOrder(startOrder + i)
                        .build())
                .collect(Collectors.toList());

        List<BoardImage> saved = boardImageRepository.saveAll(images);

        return saved.stream()
                .map(img -> BoardImageDTO.builder()
                        .imageId(img.getImageId())
                        .boardId(img.getBoardId())
                        .imageUrl(img.getImageUrl())
                        .sortOrder(img.getSortOrder())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<BoardImageDTO> listByBoard(Long boardId) {

        List<BoardImage> result = boardImageRepository.findByBoardIdOrderBySortOrderAsc(boardId);

        return result.stream()
                .map(img -> BoardImageDTO.builder()
                        .imageId(img.getImageId())
                        .boardId(img.getBoardId())
                        .imageUrl(img.getImageUrl())
                        .sortOrder(img.getSortOrder())
                        .build())
                .collect(Collectors.toList());
    }

}
