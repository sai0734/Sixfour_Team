package com.wedding.product.service;

import com.wedding.global.dto.PageRequestDTO;
import com.wedding.global.dto.PageResponseDTO;
import com.wedding.member.domain.Member;
import com.wedding.product.domain.Product;
import com.wedding.product.domain.ProductImage;
import com.wedding.product.domain.Qna;
import com.wedding.product.dto.AdminQnaListDTO;
import com.wedding.product.dto.QnaDTO;
import com.wedding.product.repository.QnaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class QnaServiceImpl implements QnaService {

    private final QnaRepository qnaRepository;

    // 상품 질문 목록 조회 (답변 포함)
    @Override
    public List<QnaDTO> listByProduct(Long pno, String currentMemberEmail) {

        List<Qna> questions = qnaRepository.listQuestionsByProduct(pno);

        return questions.stream()
                .map(question -> toDTO(question, pno))
                .collect(Collectors.toList());
    }

    private QnaDTO toDTO(Qna question, Long pno) {

        List<Qna> answerEntities = qnaRepository.listAnswersByQuestion(question.getQno());

        List<QnaDTO> answers = answerEntities.stream()
                .map(answer -> buildDTO(answer, pno, null))
                .collect(Collectors.toList());

        return buildDTO(question, pno, answers);
    }

    private QnaDTO buildDTO(Qna qna, Long pno, List<QnaDTO> answers) {

        return QnaDTO.builder()
                .qno(qna.getQno())
                .pno(pno)
                .memberEmail(qna.getMember().getEmail())
                .nickname(qna.getMember().getNickname())
                .content(qna.getContent())
                .regDate(qna.getRegDate())
                .answers(answers)
                .build();
    }

    // 관리자용 - 답변 없는 질문 목록 (상품 무관, 전체)
    @Override
    public PageResponseDTO<AdminQnaListDTO> listUnanswered(PageRequestDTO requestDTO) {

        Pageable pageable = PageRequest.of(requestDTO.getPage() - 1, requestDTO.getSize(), Sort.by("qno").descending());

        Page<Qna> result = qnaRepository.findUnansweredQuestions(pageable);

        List<AdminQnaListDTO> dtoList = result.get().map(q -> AdminQnaListDTO.builder()
                .qno(q.getQno())
                .pno(q.getProduct().getPno())
                .pname(q.getProduct().getPname())
                .thumbnail(firstImageFileName(q.getProduct()))
                .memberEmail(q.getMember().getEmail())
                .nickname(q.getMember().getNickname())
                .content(q.getContent())
                .regDate(q.getRegDate())
                .build()
        ).collect(Collectors.toList());

        return PageResponseDTO.<AdminQnaListDTO>withAll()
                .dtoList(dtoList)
                .pageRequestDTO(requestDTO)
                .totalCount(result.getTotalElements())
                .build();
    }

    // 상품 대표 이미지(ord 가장 낮은 것) 파일명. 이미지가 없으면 null
    private String firstImageFileName(Product product) {
        return product.getImageList().stream()
                .min(Comparator.comparingInt(ProductImage::getOrd))
                .map(ProductImage::getFileName)
                .orElse(null);
    }

    // 질문 등록 (로그인 회원 누구나 가능)
    @Override
    public Long register(String memberEmail, Long pno, String content) {

        Member member = Member.builder().email(memberEmail).build();
        Product product = Product.builder().pno(pno).build();

        Qna qna = Qna.builder()
                .product(product)
                .member(member)
                .content(content)
                .build();

        Qna saved = qnaRepository.save(qna);

        return saved.getQno();
    }

    // 질문 수정 (본인만 가능)
    @Override
    public void modify(String memberEmail, Long pno, Long qno, String content) {

        Qna qna = qnaRepository.findOneByProduct(qno, pno)
                .orElseThrow(() -> new NoSuchElementException("질문이 존재하지 않습니다. qno=" + qno));

        if (!qna.getMember().getEmail().equals(memberEmail)) {
            throw new IllegalStateException("본인의 질문만 수정할 수 있습니다.");
        }

        qna.changeContent(content);

        qnaRepository.save(qna);
    }

    // 관리자 답변 등록
    @Override
    public Long reply(String adminEmail, Long pno, Long parentQno, String content) {

        Qna parent = qnaRepository.findOneByProduct(parentQno, pno)
                .orElseThrow(() -> new NoSuchElementException("질문이 존재하지 않습니다. qno=" + parentQno));

        Member admin = Member.builder().email(adminEmail).build();

        Qna reply = Qna.builder()
                .product(parent.getProduct())
                .member(admin)
                .qna(parent)
                .content(content)
                .build();

        Qna saved = qnaRepository.save(reply);

        return saved.getQno();
    }

    // 관리자 답변 수정
    @Override
    public void modifyReply(Long pno, Long qno, String content) {

        Qna reply = qnaRepository.findOneByProduct(qno, pno)
                .orElseThrow(() -> new NoSuchElementException("답변이 존재하지 않습니다. qno=" + qno));

        reply.changeContent(content);

        qnaRepository.save(reply);
    }

    // 질문/답변 삭제 (본인 또는 관리자만 가능)
    @Override
    public void remove(String memberEmail, boolean isAdmin, Long pno, Long qno) {

        Qna qna = qnaRepository.findOneByProduct(qno, pno)
                .orElseThrow(() -> new NoSuchElementException("질문이 존재하지 않습니다. qno=" + qno));

        if (!isAdmin && !qna.getMember().getEmail().equals(memberEmail)) {
            throw new IllegalStateException("본인의 질문만 삭제할 수 있습니다.");
        }

        qnaRepository.deleteById(qno);
    }

}