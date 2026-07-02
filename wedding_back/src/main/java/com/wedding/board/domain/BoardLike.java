package com.wedding.board.domain;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_board_like", uniqueConstraints = {
        // 같은 회원이 같은 글에 좋아요 중복 등록 방지
        @UniqueConstraint(columnNames = {"board_id", "member_email"})
})
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BoardLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long likeId;

    @Column(name = "board_id")
    private Long boardId;

    @Column(name = "member_email")
    private String memberEmail;

    @Builder.Default
    private LocalDateTime regDate = LocalDateTime.now();

}
