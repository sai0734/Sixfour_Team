package com.wedding.faq.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FaqDTO {

    private Long faqId;

    private String category;

    private String question;

    private String answer;

    private int likeCount;

    private int sortOrder;
}
