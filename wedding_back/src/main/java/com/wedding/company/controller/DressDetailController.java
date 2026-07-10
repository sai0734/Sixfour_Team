package com.wedding.company.controller;

import com.wedding.company.dto.DressDetailDTO;
import com.wedding.company.service.DressDetailService;
import jakarta.persistence.EntityNotFoundException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

  @PutMapping("/{cmno}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> update(
      @PathVariable Long cmno,
      @RequestBody DressDetailDTO dto) {
    dressDetailService.update(cmno, dto);
    return ResponseEntity.ok().build();
  }

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<Map<String, String>> handleNotFound(EntityNotFoundException e) {
    return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
  }
}
