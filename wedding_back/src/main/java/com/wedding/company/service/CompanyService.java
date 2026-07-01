package com.wedding.company.service;

import com.wedding.company.dto.CompanyDTO;
import com.wedding.company.dto.CompanyListDTO;
import com.wedding.company.dto.CompanySearchDTO;
import com.wedding.global.dto.PageResponseDTO;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface CompanyService {

  PageResponseDTO<CompanyListDTO> getList(CompanySearchDTO searchDTO);

  Long register(CompanyDTO companyDTO);

  CompanyDTO get(Long cno);

  void modify(CompanyDTO companyDTO);

  void remove(Long cno);
}
