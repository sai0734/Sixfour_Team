package com.wedding.company.service;

import com.wedding.company.dto.HallDetailDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HallDetailServiceImpl implements HallDetailService {

  private final CompanyService companyService;

  @Override
  public HallDetailDTO get(Long cmno) {
    return companyService.get(cmno).getHallDetail();
  }
}
