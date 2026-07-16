package com.wedding.product.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 관리자 "답변 안 된 상품 Q&A" 목록용 - QnaDTO와 달리 상품명(pname)을 포함
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminQnaListDTO {

    private Long qno;
    private Long pno;
    private String pname;
    private String memberEmail;
    private String nickname;
    private String content;
    private LocalDateTime regDate;

}
