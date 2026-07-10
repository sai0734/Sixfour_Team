package com.wedding.company.service;

import com.wedding.company.dto.CompanyDTO;
import com.wedding.company.dto.CompanyListDTO;
import com.wedding.company.dto.CompanySearchDTO;
import com.wedding.global.dto.PageResponseDTO;

public interface CompanyService {

  PageResponseDTO<CompanyListDTO> getList(CompanySearchDTO searchDTO);

  Long register(CompanyDTO companyDTO);

  CompanyDTO get(Long cmno);

  void modify(CompanyDTO companyDTO);

  void remove(Long cmno);
}
