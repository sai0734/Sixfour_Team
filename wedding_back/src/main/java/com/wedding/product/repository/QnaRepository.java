package com.wedding.product.repository;

import com.wedding.product.domain.Qna;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QnaRepository extends JpaRepository<Qna, Long> {

    // 상품의 질문 목록 조회 (답변 제외, 최신순)
    @EntityGraph(attributePaths = {"member"})
    @Query("select q from Qna q where q.product.pno = :pno and q.qna is null order by q.qno desc")
    List<Qna> listQuestionsByProduct(@Param("pno") Long pno);

    // 특정 질문에 달린 답변 목록 조회
    @EntityGraph(attributePaths = {"member"})
    @Query("select q from Qna q where q.qna.qno = :qno order by q.qno asc")
    List<Qna> listAnswersByQuestion(@Param("qno") Long qno);

    // 질문/답변 1개 조회 (상품 소속 검증 포함)
    @Query("select q from Qna q where q.qno = :qno and q.product.pno = :pno")
    Optional<Qna> findOneByProduct(@Param("qno") Long qno, @Param("pno") Long pno);

    // 관리자 대시보드용 집계 - 답변 없는 질문 개수 (질문인데 자신을 참조하는 답변 row가 없는 것)
    @Query("select count(q) from Qna q where q.qna is null " +
            "and not exists (select 1 from Qna a where a.qna = q)")
    long countUnansweredQuestions();

    // 관리자용 - 답변 없는 질문 목록 (상품/작성자 정보 즉시 로딩)
    @EntityGraph(attributePaths = {"member", "product"})
    @Query(value = "select q from Qna q where q.qna is null " +
            "and not exists (select 1 from Qna a where a.qna = q)",
            countQuery = "select count(q) from Qna q where q.qna is null " +
                    "and not exists (select 1 from Qna a where a.qna = q)")
    Page<Qna> findUnansweredQuestions(Pageable pageable);

}