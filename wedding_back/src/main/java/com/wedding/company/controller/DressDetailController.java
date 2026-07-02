package com.wedding.company.controller;

import com.wedding.company.dto.DressDetailDTO;
import com.wedding.company.service.DressDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/companies/dresses")
public class DressDetailController {

  private final DressDetailService dressDetailService;

  @GetMapping("/{cmno}")
  public DressDetailDTO get(@PathVariable Long cmno) {
    return dressDetailService.get(cmno);
  }
}
