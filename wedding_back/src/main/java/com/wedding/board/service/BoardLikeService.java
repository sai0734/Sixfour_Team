package com.wedding.board.service;

public interface BoardLikeService {

    // 좋아요 등록. 이미 눌렀으면 예외
    void like(Long boardId, String memberEmail);

    // 좋아요 취소
    void unlike(Long boardId, String memberEmail);

    // 특정 회원이 이 글에 좋아요를 눌렀는지 여부 (하트 채워서 보여줄 때 사용)
    boolean isLiked(Long boardId, String memberEmail);

}
