package com.wedding.company.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedding.company.dto.CompanyDTO;
import com.wedding.company.dto.CompanyListDTO;
import com.wedding.company.dto.CompanySearchDTO;
import com.wedding.company.service.CompanyService;
import com.wedding.global.dto.PageResponseDTO;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
  public List<Map<String, Object>> getDummyCompanies() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();

    List<Map<String, Object>> companies = objectMapper.readValue(
        new ClassPathResource("data/company.json").getInputStream(),
        new TypeReference<List<Map<String, Object>>>() {});

    companies.removeIf(company ->
        "STUDIO".equals(company.get("category"))
            && ((List<?>) company.getOrDefault("imageList", List.of())).isEmpty());

    JsonNode studios = objectMapper.readTree(
        new ClassPathResource("data/studio.json").getInputStream());

    long nextCmno = companies.stream()
        .map(company -> ((Number) company.get("cmno")).longValue())
        .max(Long::compareTo)
        .orElse(0L) + 1;

    for (JsonNode studio : studios) {
      companies.add(toCompanyDummy(studio, nextCmno++));
    }

    return companies;
  }

  private Map<String, Object> toCompanyDummy(JsonNode studio, long cmno) {
    Map<String, Object> company = new LinkedHashMap<>();
    company.put("cmno", cmno);
    company.put("category", "STUDIO");
    company.put("name", studio.path("name").asText());
    company.put("ceoName", studio.path("owner").asText("미확인"));
    company.put("phone", studio.path("phone").asText());
    company.put("address", studio.path("address").asText());
    company.put("latitude", studio.path("lat").asDouble());
    company.put("longitude", studio.path("lng").asDouble());
    company.put("description", studio.path("description").asText());
    company.put("priceAvg", parsePriceAvg(studio.path("priceRange").asText()));
    company.put("delFlag", false);
    company.put("imageList", toImageList(studio.path("uploadFileNames")));
    return company;
  }

  private List<Map<String, Object>> toImageList(JsonNode uploadFileNames) {
    List<Map<String, Object>> imageList = new ArrayList<>();
    for (int i = 0; i < uploadFileNames.size(); i++) {
      Map<String, Object> image = new LinkedHashMap<>();
      image.put("fileName", uploadFileNames.get(i).asText());
      image.put("ord", i);
      imageList.add(image);
    }
    return imageList;
  }

  private BigDecimal parsePriceAvg(String priceRange) {
    String number = priceRange.replaceAll("[^0-9].*", "");
    if (number.isBlank()) {
      return BigDecimal.ZERO;
    }
    return BigDecimal.valueOf(Long.parseLong(number) * 10000L);
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
