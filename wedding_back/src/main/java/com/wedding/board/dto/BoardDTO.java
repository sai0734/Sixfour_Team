package com.wedding.board.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BoardDTO {

    private Long boardId;

    private String memberEmail;

    private String nickname;

    private String boardType;

    private String category;

    private String title;

    private String content;

    private String aiSummary;

    private Integer rating;

    private int viewCount;

    private int likeCount;

    private int commentCount;

    private boolean deleted;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime regDate;
}
