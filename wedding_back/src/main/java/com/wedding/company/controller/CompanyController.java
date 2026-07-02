package com.wedding.company.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedding.company.dto.CompanyDTO;
import com.wedding.company.dto.CompanyListDTO;
import com.wedding.company.dto.CompanySearchDTO;
import com.wedding.company.service.CompanyService;
import com.wedding.global.dto.PageResponseDTO;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/companies")
public class CompanyController {

  private final CompanyService companyService;

  @GetMapping("/list")
  public PageResponseDTO<CompanyListDTO> list(CompanySearchDTO searchDTO) {
    log.info(searchDTO);
    return companyService.getList(searchDTO);
  }

  @GetMapping("/dummy")
  public List<Object> getDummyCompanies() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();

    return objectMapper.readValue(
        new ClassPathResource("data/company.json").getInputStream(),
        new TypeReference<List<Object>>() {});
  }

  @GetMapping("/{cmno}")
  public CompanyDTO get(@PathVariable Long cmno) {
    return companyService.get(cmno);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping("/")
  public Map<String, Long> register(@RequestBody CompanyDTO companyDTO) {
    Long cmno = companyService.register(companyDTO);
    return Map.of("cmno", cmno);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PutMapping("/{cmno}")
  public Map<String, String> modify(@PathVariable Long cmno, @RequestBody CompanyDTO companyDTO) {
    companyDTO.setCmno(cmno);
    companyService.modify(companyDTO);
    return Map.of("RESULT", "SUCCESS");
  }

  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping("/{cmno}")
  public Map<String, String> remove(@PathVariable Long cmno) {
    companyService.remove(cmno);
    return Map.of("RESULT", "SUCCESS");
  }
}
