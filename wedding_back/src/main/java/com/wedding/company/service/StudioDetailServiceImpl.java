package com.wedding.company.service;

import com.wedding.company.dto.StudioDetailDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudioDetailServiceImpl implements StudioDetailService {

  private final CompanyService companyService;

  @Override
  public StudioDetailDTO get(Long cmno) {
    return companyService.get(cmno).getStudioDetail();
  }
}
