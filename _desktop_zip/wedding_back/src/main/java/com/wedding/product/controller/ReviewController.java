package com.wedding.product.controller;

import com.wedding.global.util.CustomFileUtil;
import com.wedding.product.dto.ReviewDTO;
import com.wedding.product.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@Log4j2
@RequiredArgsConstructor
@RequestMapping("/api/product")
public class ReviewController {

    private final ReviewService reviewService;
    private final CustomFileUtil customFileUtil;

    // 상품 리뷰 목록 조회 (누구나 가능, 로그인했으면 본인 리뷰 표시용 정보 포함)
    @GetMapping("/{pno}/reviews")
    public List<ReviewDTO> listByProduct(@PathVariable Long pno, Principal principal) {

        log.info("ReviewController_listByProduct 실행~~~~~~~~");

        String currentMemberEmail = principal != null ? principal.getName() : null;

        return reviewService.listByProduct(pno, currentMemberEmail);
    }

    // 리뷰 작성 자격 확인 (로그인 필수)
    @PreAuthorize("hasAnyRole('USER')")
    @GetMapping("/{pno}/reviews/eligibility")
    public Map<String, String> checkReviewEligibility(Principal principal, @PathVariable Long pno) {

        reviewService.checkReviewEligibility(principal.getName(), pno);

        return Map.of("RESULT", "SUCCESS");
    }

    // 리뷰 작성 (구매 회원만, 사진 첨부 가능)
    @PreAuthorize("hasAnyRole('USER')")
    @PostMapping("/{pno}/reviews")
    public Map<String, Long> register(Principal principal, @PathVariable Long pno,
                                      @RequestParam(required = false) Integer rating,
                                      @RequestParam String content,
                                      @RequestParam(required = false) List<MultipartFile> files) {

        log.info("ReviewController_register 실행~~~~~~~~");

        List<String> uploadFileNames = customFileUtil.saveFiles(files);

        Long rno = reviewService.register(principal.getName(), pno, rating, content, uploadFileNames);

        return Map.of("rno", rno);
    }

    // 리뷰 수정 (본인만 가능, 기존 유지 파일 + 새 첨부 파일을 합쳐서 최종 목록으로 넘김)
    @PreAuthorize("hasAnyRole('USER')")
    @PutMapping("/{pno}/reviews/{rno}")
    public Map<String, String> modify(Principal principal, @PathVariable Long pno, @PathVariable Long rno,
                                      @RequestParam(required = false) Integer rating,
                                      @RequestParam String content,
                                      @RequestParam(required = false) List<String> keepFileNames,
                                      @RequestParam(required = false) List<MultipartFile> files) {

        log.info("ReviewController_modify 실행~~~~~~~~");

        List<String> newUploadFileNames = customFileUtil.saveFiles(files);

        List<String> finalFileNames = new ArrayList<>();
        if (keepFileNames != null) {
            finalFileNames.addAll(keepFileNames);
        }
        if (newUploadFileNames != null) {
            finalFileNames.addAll(newUploadFileNames);
        }

        reviewService.modify(principal.getName(), pno, rno, rating, content, finalFileNames);

        return Map.of("RESULT", "SUCCESS");
    }

    // 관리자 답변 등록
    @PreAuthorize("hasAnyRole('ADMIN')")
    @PostMapping("/{pno}/reviews/{rno}/reply")
    public Map<String, Long> reply(Principal principal, @PathVariable Long pno, @PathVariable Long rno,
                                   @RequestParam String content) {

        log.info("ReviewController_reply 실행~~~~~~~~");

        Long replyRno = reviewService.reply(principal.getName(), pno, rno, content);

        return Map.of("rno", replyRno);
    }

    // 관리자 답변 수정
    @PreAuthorize("hasAnyRole('ADMIN')")
    @PutMapping("/{pno}/reviews/{rno}/reply")
    public Map<String, String> modifyReply(@PathVariable Long pno, @PathVariable Long rno,
                                           @RequestParam String content) {

        log.info("ReviewController_modifyReply 실행~~~~~~~~");

        reviewService.modifyReply(pno, rno, content);

        return Map.of("RESULT", "SUCCESS");
    }

    // 리뷰 삭제 (본인 또는 관리자)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @DeleteMapping("/{pno}/reviews/{rno}")
    public Map<String, String> remove(Authentication authentication, @PathVariable Long pno, @PathVariable Long rno) {

        log.info("ReviewController_remove 실행~~~~~~~~");

        String email = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        reviewService.remove(email, isAdmin, pno, rno);

        return Map.of("RESULT", "SUCCESS");
    }

    // 회원 본인 리뷰 목록 (마이페이지용)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/reviews/my")
    public List<ReviewDTO> listMyReviews(Principal principal) {

        log.info("ReviewController_listMyReviews 실행~~~~~~~~");

        return reviewService.listByMember(principal.getName());

    }

}