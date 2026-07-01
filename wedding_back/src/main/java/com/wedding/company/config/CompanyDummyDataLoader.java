package com.wedding.company.config;

import com.wedding.company.domain.Company;
import com.wedding.company.domain.CompanyType;
import com.wedding.company.domain.DressDetail;
import com.wedding.company.domain.HallDetail;
import com.wedding.company.domain.MakeupDetail;
import com.wedding.company.domain.StudioDetail;
import com.wedding.company.repository.CompanyRepository;
import com.wedding.company.repository.DressDetailRepository;
import com.wedding.company.repository.HallDetailRepository;
import com.wedding.company.repository.MakeupDetailRepository;
import com.wedding.company.repository.StudioDetailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Log4j2
public class CompanyDummyDataLoader implements CommandLineRunner {

  private static final String DUMMY_PATH = "dummy/";

  private final CompanyRepository companyRepository;
  private final HallDetailRepository hallDetailRepository;
  private final DressDetailRepository dressDetailRepository;
  private final MakeupDetailRepository makeupDetailRepository;
  private final StudioDetailRepository studioDetailRepository;

  @Override
  @Transactional
  public void run(String... args) {

    if (companyRepository.count() > 0) {
      return;
    }

    Map<Long, Company> companyMap = loadCompanies();
    loadCompanyImages(companyMap);
    loadHallDetails(companyMap);
    loadDressDetails(companyMap);
    loadMakeupDetails(companyMap);
    loadStudioDetails(companyMap);

    log.info("Company dummy data loaded: " + companyMap.size());
  }

  private Map<Long, Company> loadCompanies() {

    Map<Long, Company> companyMap = new HashMap<>();

    for (Map<String, String> row : readCsv("tbl_company.csv")) {
      Long cmno = toLong(row.get("cmno"));
      String address = row.get("address");

      Company company = Company.builder()
          .name(row.get("name"))
          .ceoName(row.get("ceoName"))
          .type(toCompanyType(row.get("category")))
          .region(extractRegion(address))
          .address(address)
          .latitude(toDouble(row.get("latitude")))
          .longitude(toDouble(row.get("longitude")))
          .phone(row.get("phone"))
          .price(toInt(row.get("priceAvg")))
          .description(row.get("description"))
          .delFlag(Boolean.parseBoolean(row.getOrDefault("delFlag", "false")))
          .ratingAvg(0.0)
          .reviewCount(0)
          .viewCount(0L)
          .build();

      Company savedCompany = companyRepository.save(company);
      companyMap.put(cmno, savedCompany);
    }

    return companyMap;
  }

  private void loadCompanyImages(Map<Long, Company> companyMap) {

    for (Map<String, String> row : readCsv("company_image_list.csv")) {
      Company company = companyMap.get(toLong(row.get("company_cmno")));
      addCompanyImage(company, row.get("file_name"));
    }
  }

  private void loadHallDetails(Map<Long, Company> companyMap) {

    for (Map<String, String> row : readCsv("tbl_hall_detail.csv")) {
      Company company = companyMap.get(toLong(row.get("cmno")));
      if (company == null) {
        continue;
      }

      HallDetail detail = HallDetail.builder()
          .company(company)
          .build();
      detail.changeDetail(row.get("hallType"), row.get("representative"),
          null, null, null, null, row.get("description"), row.get("imageUrl"));
      hallDetailRepository.save(detail);
      addCompanyImage(company, row.get("imageUrl"));
    }

    for (Map<String, String> row : readCsv("tbl_hall_item.csv")) {
      Company company = companyMap.get(toLong(row.get("cmno")));
      addCompanyImage(company, row.get("imageUrl"));
    }
  }

  private void loadDressDetails(Map<Long, Company> companyMap) {

    for (Map<String, String> row : readCsv("tbl_dress_detail.csv")) {
      Company company = companyMap.get(toLong(row.get("cmno")));
      if (company == null) {
        continue;
      }

      DressDetail detail = DressDetail.builder()
          .company(company)
          .build();
      detail.changeDetail(null, null, null, null, row.get("sizeRange"));
      dressDetailRepository.save(detail);
    }

    for (Map<String, String> row : readCsv("tbl_dress_item.csv")) {
      Company company = companyMap.get(toLong(row.get("cmno")));
      addCompanyImage(company, row.get("imageUrl"));
    }
  }

  private void loadMakeupDetails(Map<Long, Company> companyMap) {

    for (Map<String, String> row : readCsv("tbl_makeup_detail.csv")) {
      Company company = companyMap.get(toLong(row.get("cmno")));
      if (company == null) {
        continue;
      }

      MakeupDetail detail = MakeupDetail.builder()
          .company(company)
          .build();
      detail.changeDetail(toBoolean(row.get("includesHairService")),
          toBoolean(row.get("includesMakeupService")),
          toBoolean(row.get("includesNailService")),
          null, null, null,
          toInt(row.get("hairPrice")),
          toInt(row.get("makeupPrice")),
          toInt(row.get("nailPrice")));
      makeupDetailRepository.save(detail);
    }
  }

  private void loadStudioDetails(Map<Long, Company> companyMap) {

    for (Map<String, String> row : readCsv("tbl_studio_detail.csv")) {
      Company company = companyMap.get(toLong(row.get("cmno")));
      if (company == null) {
        continue;
      }

      StudioDetail detail = StudioDetail.builder()
          .company(company)
          .build();
      detail.changeDetail(null, row.get("themeTags"), null, null, null);
      studioDetailRepository.save(detail);
    }
  }

  private void addCompanyImage(Company company, String fileName) {
    if (company == null || fileName == null || fileName.isBlank()) {
      return;
    }

    boolean exists = company.getImageList().stream()
        .anyMatch(image -> fileName.equals(image.getFileName()));

    if (!exists) {
      company.addImageString(fileName);
    }
  }

  private List<Map<String, String>> readCsv(String fileName) {

    List<Map<String, String>> rows = new ArrayList<>();
    ClassPathResource resource = new ClassPathResource(DUMMY_PATH + fileName);

    if (!resource.exists()) {
      return rows;
    }

    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

      String headerLine = reader.readLine();
      if (headerLine == null) {
        return rows;
      }

      String[] headers = headerLine.split(",", -1);
      String line;

      while ((line = reader.readLine()) != null) {
        String[] values = splitCsvLine(line, headers.length);
        Map<String, String> row = new LinkedHashMap<>();

        for (int i = 0; i < headers.length; i++) {
          String value = i < values.length ? values[i] : "";
          row.put(headers[i], value);
        }

        rows.add(row);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Failed to read dummy csv: " + fileName, e);
    }

    return rows;
  }

  private String[] splitCsvLine(String line, int expectedColumns) {

    String[] values = line.split(",", -1);

    if (values.length <= expectedColumns) {
      return values;
    }

    String[] normalized = new String[expectedColumns];

    for (int i = 0; i < expectedColumns - 1; i++) {
      normalized[i] = values[i];
    }

    StringBuilder lastValue = new StringBuilder();
    for (int i = expectedColumns - 1; i < values.length; i++) {
      if (lastValue.length() > 0) {
        lastValue.append(",");
      }
      lastValue.append(values[i]);
    }
    normalized[expectedColumns - 1] = lastValue.toString();

    return normalized;
  }

  private CompanyType toCompanyType(String value) {
    if (value == null || value.isBlank()) {
      return CompanyType.HALL;
    }
    return CompanyType.valueOf(value.trim().toUpperCase());
  }

  private String extractRegion(String address) {
    if (address == null || address.isBlank()) {
      return null;
    }
    return address.split("\\s+")[0];
  }

  private Long toLong(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return Long.parseLong(value.trim());
  }

  private Integer toInt(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return Integer.parseInt(value.trim());
  }

  private Double toDouble(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return Double.parseDouble(value.trim());
  }

  private Boolean toBoolean(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return Boolean.parseBoolean(value.trim());
  }
}
