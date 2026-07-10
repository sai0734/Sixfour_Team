package com.wedding.company.controller;

import com.wedding.company.dto.StudioDetailDTO;
import com.wedding.company.service.StudioDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/companies/studios")
public class StudioDetailController {

  private final StudioDetailService studioDetailService;

  @GetMapping("/{cmno}")
  public StudioDetailDTO get(@PathVariable Long cmno) {
    return studioDetailService.get(cmno);
  }
}
