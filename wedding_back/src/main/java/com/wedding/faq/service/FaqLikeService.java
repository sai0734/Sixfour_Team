package com.wedding.faq.service;

public interface FaqLikeService {

    void like(Long faqId, String memberEmail);

    void unlike(Long faqId, String memberEmail);

    boolean isLiked(Long faqId, String memberEmail);

}
