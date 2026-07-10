package com.wedding.faq.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import com.wedding.faq.domain.Faq;
import com.wedding.faq.dto.FaqDTO;
import com.wedding.faq.repository.FaqRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class FaqServiceImpl implements FaqService {

    private final FaqRepository faqRepository;

    private final ModelMapper modelMapper;

    @Override
    public Long register(FaqDTO faqDTO) {

        log.info("faq register.........");

        Faq faq = modelMapper.map(faqDTO, Faq.class);

        Faq saved = faqRepository.save(faq);

        return saved.getFaqId();
    }

    @Override
    public FaqDTO get(Long faqId) {

        Optional<Faq> result = faqRepository.findById(faqId);

        Faq faq = result.orElseThrow();

        return modelMapper.map(faq, FaqDTO.class);
    }

    @Override
    public void modify(FaqDTO faqDTO) {

        Optional<Faq> result = faqRepository.findById(faqDTO.getFaqId());

        Faq faq = result.orElseThrow();

        faq.changeCategory(faqDTO.getCategory());
        faq.changeQuestion(faqDTO.getQuestion());
        faq.changeAnswer(faqDTO.getAnswer());
        faq.changeSortOrder(faqDTO.getSortOrder());

        faqRepository.save(faq);
    }

    @Override
    public void remove(Long faqId) {

        faqRepository.deleteById(faqId);
    }

    @Override
    public List<FaqDTO> listAll() {

        List<Faq> result = faqRepository.findAllByOrderBySortOrderAsc();

        return result.stream()
                .map(f -> modelMapper.map(f, FaqDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<FaqDTO> listByCategory(String category) {

        List<Faq> result = faqRepository.findByCategoryOrderBySortOrderAsc(category);

        return result.stream()
                .map(f -> modelMapper.map(f, FaqDTO.class))
                .collect(Collectors.toList());
    }

}
