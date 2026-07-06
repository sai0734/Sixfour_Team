package com.wedding.faq.service;

import java.util.List;

import com.wedding.faq.dto.FaqDTO;

public interface FaqService {

    Long register(FaqDTO faqDTO);

    FaqDTO get(Long faqId);

    void modify(FaqDTO faqDTO);

    void remove(Long faqId);

    List<FaqDTO> listAll();

    List<FaqDTO> listByCategory(String category);

}
