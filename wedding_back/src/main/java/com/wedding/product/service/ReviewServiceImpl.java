package com.wedding.product.service;

import com.wedding.checkout.domain.OrderItem;
import com.wedding.checkout.repository.OrderItemRepository;
import com.wedding.member.domain.Member;
import com.wedding.product.domain.Product;
import com.wedding.product.domain.Review;
import com.wedding.product.dto.ReviewDTO;
import com.wedding.product.repository.ProductRepository;
import com.wedding.product.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    // 상품 리뷰 목록 조회 (답변 포함, 계층 구조)
    @Override
    public List<ReviewDTO> listByProduct(Long pno, String currentMemberEmail) {

        List<Review> topReviews = reviewRepository.listTopReviewsByProduct(pno);

        return topReviews.stream()
                .map(review -> toDTO(review, pno, currentMemberEmail))
                .collect(Collectors.toList());
    }

    private ReviewDTO toDTO(Review review, Long pno, String currentMemberEmail) {

        List<Review> replyEntities = reviewRepository.listRepliesByReview(review.getRno());

        List<ReviewDTO> replies = replyEntities.stream()
                .map(reply -> buildDTO(reply, pno, currentMemberEmail, null))
                .collect(Collectors.toList());

        return buildDTO(review, pno, currentMemberEmail, replies);
    }

    private ReviewDTO buildDTO(Review review, Long pno, String currentMemberEmail, List<ReviewDTO> replies) {

        List<String> fileNames = review.getImageList().stream()
                .map(img -> img.getFileName())
                .collect(Collectors.toList());

        return ReviewDTO.builder()
                .rno(review.getRno())
                .pno(pno)
                .memberEmail(review.getMember().getEmail())
                .nickname(review.getMember().getNickname())
                .rating(review.getRating())
                .content(review.getContent())
                .uploadFileNames(fileNames)
                .regDate(review.getRegDate())
                .isMine(review.getMember().getEmail().equals(currentMemberEmail))
                .replies(replies)
                .build();
    }

    // 리뷰 작성 (구매 이력 있는 회원만 가능)
    @Override
    public Long register(String memberEmail, Long pno, Integer rating, String content, List<String> uploadFileNames) {

        OrderItem orderItem = orderItemRepository.findAvailableOrderItem(memberEmail, pno)
                .orElseThrow(() -> new IllegalStateException("리뷰는 구매 회원만 작성이 가능합니다."));

        Member member = Member.builder().email(memberEmail).build();
        Product product = Product.builder().pno(pno).build();

        Review review = Review.builder()
                .product(product)
                .member(member)
                .orderItem(orderItem)
                .rating(rating)
                .content(content)
                .build();

        if (uploadFileNames != null) {
            uploadFileNames.forEach(fileName -> review.addImageString(fileName));
        }

        Review saved = reviewRepository.save(review);

        refreshProductRatingStats(pno);

        return saved.getRno();
    }

    // 리뷰 수정 (본인만 가능, 평점/내용 변경, 사진은 컨트롤러가 넘겨준 최종 목록으로 통째 교체)
    @Override
    public void modify(String memberEmail, Long pno, Long rno, Integer rating, String content, List<String> uploadFileNames) {

        Review review = reviewRepository.findOneByProduct(rno, pno)
                .orElseThrow(() -> new NoSuchElementException("리뷰가 존재하지 않습니다. rno=" + rno));

        if (!review.getMember().getEmail().equals(memberEmail)) {
            throw new IllegalStateException("본인의 리뷰만 수정할 수 있습니다.");
        }

        if (review.getRating() == null) {
            throw new IllegalStateException("답변은 이 방식으로 수정할 수 없습니다.");
        }

        review.changeRating(rating);
        review.changeContent(content);

        review.clearImageList();
        if (uploadFileNames != null) {
            uploadFileNames.forEach(fileName -> review.addImageString(fileName));
        }

        reviewRepository.save(review);

        refreshProductRatingStats(pno);
    }

    // 관리자 답변 등록 (부모 리뷰와 동일한 orderItem을 참조해 FK 제약 만족)
    @Override
    public Long reply(String adminEmail, Long pno, Long parentRno, String content) {

        Review parent = reviewRepository.findOneByProduct(parentRno, pno)
                .orElseThrow(() -> new NoSuchElementException("리뷰가 존재하지 않습니다. rno=" + parentRno));

        Member admin = Member.builder().email(adminEmail).build();

        Review reply = Review.builder()
                .product(parent.getProduct())
                .member(admin)
                .orderItem(parent.getOrderItem())
                .review(parent)
                .rating(null)
                .content(content)
                .build();

        Review saved = reviewRepository.save(reply);

        return saved.getRno();
    }

    // 관리자 답변 수정
    @Override
    public void modifyReply(Long pno, Long rno, String content) {

        Review reply = reviewRepository.findOneByProduct(rno, pno)
                .orElseThrow(() -> new NoSuchElementException("답변이 존재하지 않습니다. rno=" + rno));

        if (reply.getRating() != null) {
            throw new IllegalStateException("일반 리뷰는 이 방식으로 수정할 수 없습니다.");
        }

        reply.changeContent(content);

        reviewRepository.save(reply);
    }

    // 리뷰 삭제 (본인 또는 관리자만 가능)
    @Override
    public void remove(String memberEmail, boolean isAdmin, Long pno, Long rno) {

        Review review = reviewRepository.findOneByProduct(rno, pno)
                .orElseThrow(() -> new NoSuchElementException("리뷰가 존재하지 않습니다. rno=" + rno));

        if (!isAdmin && !review.getMember().getEmail().equals(memberEmail)) {
            throw new IllegalStateException("본인의 리뷰만 삭제할 수 있습니다.");
        }

        boolean wasRating = review.getRating() != null;

        reviewRepository.deleteById(rno);

        if (wasRating) {
            refreshProductRatingStats(pno);
        }
    }

    // 리뷰 작성 자격 확인 (구매 이력 없으면 예외)
    @Override
    public void checkReviewEligibility(String memberEmail, Long pno) {

        orderItemRepository.findAvailableOrderItem(memberEmail, pno)
                .orElseThrow(() -> new IllegalStateException("리뷰는 구매 회원만 작성이 가능합니다."));
    }

    // 상품의 평균 평점/리뷰 개수를 다시 계산해서 Product에 반영
    private void refreshProductRatingStats(Long pno) {

        Double avg = reviewRepository.getAverageRating(pno);
        long count = reviewRepository.countByProduct(pno);

        Product product = productRepository.findById(pno)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 상품입니다. pno=" + pno));

        product.changeRatingStats(avg == null ? 0 : avg, (int) count);

        productRepository.save(product);
    }

}