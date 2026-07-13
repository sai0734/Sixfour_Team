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

  // 관리자가 회원을 이 업체의 문의 담당자로 임명/해제
  void assignManager(Long cmno, String managerEmail);

  void unassignManager(Long cmno);

  // 로그인한 회원이 담당하고 있는 업체가 있는지 확인 (없으면 null)
  CompanyDTO getManagedCompany(String memberEmail);

  // 관리자 회원관리 "담당자 탭"용 - 담당자 지정된 업체 전체 목록
  java.util.List<CompanyDTO> getManagedCompanies();
}