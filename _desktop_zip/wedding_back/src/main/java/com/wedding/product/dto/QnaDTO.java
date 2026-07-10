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
public class QnaDTO {

    private Long qno;

    private Long pno;

    private String memberEmail;

    private String nickname;

    private String content;

    private LocalDateTime regDate;

    private List<QnaDTO> answers;

}