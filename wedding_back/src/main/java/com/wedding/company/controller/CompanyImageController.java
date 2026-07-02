package com.wedding.company.controller;

import com.wedding.company.dto.CompanyDTO;
import com.wedding.company.service.CompanyService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/companies/images")
public class CompanyImageController {

  private final CompanyService companyService;

  @GetMapping("/{cmno}")
  public Map<String, List<String>> list(@PathVariable Long cmno) {
    CompanyDTO companyDTO = companyService.get(cmno);
    return Map.of("images", companyDTO.getUploadFileNames());
  }
}
