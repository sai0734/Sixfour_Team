package com.wedding.company.service;

import com.wedding.company.dto.MakeupDetailDTO;

public interface MakeupDetailService {
  MakeupDetailDTO get(Long cmno);
  void update(Long cmno, MakeupDetailDTO dto);
}
