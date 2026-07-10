package com.wedding.company.service;

import com.wedding.company.dto.DressDetailDTO;

public interface DressDetailService {
  DressDetailDTO get(Long cmno);
  void update(Long cmno, DressDetailDTO dto);
}
