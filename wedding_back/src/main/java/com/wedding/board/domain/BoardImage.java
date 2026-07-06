package com.wedding.board.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_board_image")
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BoardImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long imageId;

    @Column(name = "board_id")
    private Long boardId;

    private String imageUrl;

    private int sortOrder;

}
