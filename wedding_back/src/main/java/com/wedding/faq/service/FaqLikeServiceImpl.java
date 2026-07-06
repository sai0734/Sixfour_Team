package com.wedding.faq.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wedding.faq.domain.Faq;
import com.wedding.faq.domain.FaqLike;
import com.wedding.faq.repository.FaqLikeRepository;
import com.wedding.faq.repository.FaqRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class FaqLikeServiceImpl implements FaqLikeService {

    private final FaqLikeRepository faqLikeRepository;

    private final FaqRepository faqRepository;

    @Override
    @Transactional
    public void like(Long faqId, String memberEmail) {

        boolean exists = faqLikeRepository.existsByFaqIdAndMemberEmail(faqId, memberEmail);

        if (exists) {
            throw new IllegalStateException("이미 추천한 FAQ입니다.");
        }

        FaqLike faqLike = FaqLike.builder()
                .faqId(faqId)
                .memberEmail(memberEmail)
                .build();

        faqLikeRepository.save(faqLike);

        Faq faq = faqRepository.findById(faqId).orElseThrow();
        faq.increaseLikeCount();
        faqRepository.save(faq);
    }

    @Override
    @Transactional
    public void unlike(Long faqId, String memberEmail) {

        Optional<FaqLike> result = faqLikeRepository.findByFaqIdAndMemberEmail(faqId, memberEmail);

        FaqLike faqLike = result.orElseThrow();

        faqLikeRepository.delete(faqLike);

        Faq faq = faqRepository.findById(faqId).orElseThrow();
        faq.decreaseLikeCount();
        faqRepository.save(faq);
    }

    @Override
    public boolean isLiked(Long faqId, String memberEmail) {

        return faqLikeRepository.existsByFaqIdAndMemberEmail(faqId, memberEmail);
    }

}
