package com.wedding.company.controller;

import com.wedding.company.dto.CompanyDTO;
import com.wedding.company.service.CompanyService;
import com.wedding.global.util.CustomFileUtil;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/companies/images")
public class CompanyImageController {

  private final CompanyService companyService;
  private final CustomFileUtil fileUtil;

  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping("/upload")
  public Map<String, List<String>> upload(List<MultipartFile> files) {
    List<String> uploadFileNames = fileUtil.saveFiles(files);
    return Map.of("uploadFileNames", uploadFileNames == null ? List.of() : uploadFileNames);
  }

  @GetMapping("/{cmno}")
  public Map<String, List<String>> list(@PathVariable Long cmno) {
    CompanyDTO companyDTO = companyService.get(cmno);
    return Map.of("images", companyDTO.getUploadFileNames());
  }

  @GetMapping("/view/{fileName}")
  public ResponseEntity<Resource> view(@PathVariable String fileName) {
    return fileUtil.getFile(fileName);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping("/{fileName}")
  public Map<String, String> remove(@PathVariable String fileName) {
    fileUtil.deleteFiles(List.of(fileName));
    return Map.of("RESULT", "SUCCESS");
  }
}
