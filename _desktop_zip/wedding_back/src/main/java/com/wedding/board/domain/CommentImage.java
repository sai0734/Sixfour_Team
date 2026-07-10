package com.wedding.board.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_comment_image")
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommentImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long imageId;

    @Column(name = "comment_id")
    private Long commentId;

    private String imageUrl;

    private int sortOrder;

}
