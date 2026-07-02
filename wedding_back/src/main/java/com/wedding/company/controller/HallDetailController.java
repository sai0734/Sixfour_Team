package com.wedding.company.controller;

import com.wedding.company.dto.HallDetailDTO;
import com.wedding.company.service.HallDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/companies/halls")
public class HallDetailController {

  private final HallDetailService hallDetailService;

  @GetMapping("/{cmno}")
  public HallDetailDTO get(@PathVariable Long cmno) {
    return hallDetailService.get(cmno);
  }
}
