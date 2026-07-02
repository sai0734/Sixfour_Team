package com.wedding.company.service;

import com.wedding.company.dto.MakeupDetailDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MakeupDetailServiceImpl implements MakeupDetailService {

  private final CompanyService companyService;

  @Override
  public MakeupDetailDTO get(Long cmno) {
    return companyService.get(cmno).getMakeupDetail();
  }
}
