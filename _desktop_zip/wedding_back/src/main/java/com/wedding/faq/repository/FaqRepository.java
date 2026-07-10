package com.wedding.faq.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wedding.faq.domain.Faq;

public interface FaqRepository extends JpaRepository<Faq, Long> {

    List<Faq> findAllByOrderBySortOrderAsc();

    List<Faq> findByCategoryOrderBySortOrderAsc(String category);

}
