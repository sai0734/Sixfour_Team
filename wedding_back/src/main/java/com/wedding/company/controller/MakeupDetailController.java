package com.wedding.company.controller;

import com.wedding.company.dto.MakeupDetailDTO;
import com.wedding.company.service.MakeupDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/companies/makeup")
public class MakeupDetailController {

  private final MakeupDetailService makeupDetailService;

  @GetMapping("/{cmno}")
  public MakeupDetailDTO get(@PathVariable Long cmno) {
    return makeupDetailService.get(cmno);
  }
}
