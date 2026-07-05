package com.wedding.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReviewDTO {

    private Long rno;

    private Long pno;

    private String memberEmail;

    private String nickname;

    private Integer rating;       // 관리자 답변이면 null

    private String content;

    private List<String> uploadFileNames;

    private LocalDateTime regDate;

    private boolean isMine;       // 조회자 본인 작성 여부 (삭제버튼 노출 판단용)

    private List<ReviewDTO> replies;

}