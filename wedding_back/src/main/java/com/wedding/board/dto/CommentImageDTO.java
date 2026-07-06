package com.wedding.board.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommentImageDTO {

    private Long imageId;

    private Long commentId;

    private String imageUrl;

    private int sortOrder;
}
