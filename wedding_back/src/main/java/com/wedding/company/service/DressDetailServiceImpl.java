package com.wedding.company.service;

import com.wedding.company.dto.DressDetailDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DressDetailServiceImpl implements DressDetailService {

  private final CompanyService companyService;

  @Override
  public DressDetailDTO get(Long cmno) {
    return companyService.get(cmno).getDressDetail();
  }
}
