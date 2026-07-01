package com.wedding.company.controller;

import com.wedding.company.dto.CompanyDTO;
import com.wedding.company.dto.CompanyListDTO;
import com.wedding.company.dto.CompanySearchDTO;
import com.wedding.company.service.CompanyService;
import com.wedding.global.dto.PageResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/companies")
public class CompanyController {

  private final CompanyService companyService;

  @GetMapping("/list")
  public PageResponseDTO<CompanyListDTO> list(CompanySearchDTO searchDTO) {
    log.info("company list: " + searchDTO);
    return companyService.getList(searchDTO);
  }

  @GetMapping("/{cno}")
  public CompanyDTO read(@PathVariable Long cno) {
    return companyService.get(cno);
  }

  @PostMapping("/")
  public Map<String, Long> register(@RequestBody CompanyDTO companyDTO) {
    Long cno = companyService.register(companyDTO);
    return Map.of("result", cno);
  }

  @PutMapping("/{cno}")
  public Map<String, String> modify(@PathVariable Long cno, @RequestBody CompanyDTO companyDTO) {
    companyDTO.setCno(cno);
    companyService.modify(companyDTO);
    return Map.of("RESULT", "SUCCESS");
  }

  @DeleteMapping("/{cno}")
  public Map<String, String> remove(@PathVariable Long cno) {
    companyService.remove(cno);
    return Map.of("RESULT", "SUCCESS");
  }
}
