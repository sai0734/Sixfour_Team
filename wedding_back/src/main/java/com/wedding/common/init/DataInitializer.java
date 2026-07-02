package com.wedding.common.init;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedding.company.domain.Company;
import com.wedding.company.domain.CompanyCategory;
import com.wedding.company.domain.DressDetail;
import com.wedding.company.domain.DressItem;
import com.wedding.company.domain.DressItemType;
import com.wedding.company.domain.HallDetail;
import com.wedding.company.domain.HallItem;
import com.wedding.company.domain.HallType;
import com.wedding.company.domain.MakeupDetail;
import com.wedding.company.domain.MakeupPackage;
import com.wedding.company.domain.MakeupPackageType;
import com.wedding.company.domain.MealType;
import com.wedding.company.domain.StudioDetail;
import com.wedding.company.repository.CompanyRepository;
import com.wedding.company.repository.DressDetailRepository;
import com.wedding.company.repository.DressItemRepository;
import com.wedding.company.repository.HallDetailRepository;
import com.wedding.company.repository.HallItemRepository;
import com.wedding.company.repository.MakeupDetailRepository;
import com.wedding.company.repository.MakeupPackageRepository;
import com.wedding.company.repository.StudioDetailRepository;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Log4j2
public class DataInitializer implements ApplicationRunner {

  private final CompanyRepository companyRepository;
  private final HallDetailRepository hallDetailRepository;
  private final HallItemRepository hallItemRepository;
  private final DressDetailRepository dressDetailRepository;
  private final DressItemRepository dressItemRepository;
  private final StudioDetailRepository studioDetailRepository;
  private final MakeupDetailRepository makeupDetailRepository;
  private final MakeupPackageRepository makeupPackageRepository;
  private final ObjectMapper objectMapper;

  @Override
  @Transactional
  public void run(ApplicationArguments args) throws Exception {
    if (companyRepository.count() > 0) {
      log.info("Dummy data already exists. Skip JSON initialization.");
      return;
    }

    log.info("===== JSON dummy data initialization start =====");
    insertCompanies();
    insertHalls();
    insertDresses();
    insertStudios();
    insertMakeups();
    log.info("===== JSON dummy data initialization complete =====");
  }

  private void insertCompanies() throws Exception {
    List<Map<String, Object>> list = readJson("data/company.json");

    for (Map<String, Object> m : list) {
      Company company = Company.builder()
          .cmno(toLongObject(m.get("cmno")))
          .category(enumValue(CompanyCategory.class, m.get("category"), CompanyCategory.HALL))
          .name((String) m.get("name"))
          .ceoName((String) m.get("ceoName"))
          .phone((String) m.get("phone"))
          .address((String) m.get("address"))
          .latitude(toDoubleObject(m.get("latitude")))
          .longitude(toDoubleObject(m.get("longitude")))
          .description((String) m.get("description"))
          .priceAvg(toBigDecimal(m.get("priceAvg")))
          .delFlag(toBoolean(m.get("delFlag")))
          .build();

      List<Map<String, Object>> images = castList(m.get("imageList"));
      if (images != null) {
        images.stream()
            .sorted(Comparator.comparingInt(img -> toInt(img.get("ord"))))
            .map(img -> (String) img.get("fileName"))
            .filter(fileName -> fileName != null && !fileName.isBlank())
            .forEach(company::addImage);
      }

      companyRepository.save(company);
    }

    log.info("Inserted {} companies from JSON.", companyRepository.count());
  }

  private void insertHalls() throws Exception {
    List<Map<String, Object>> list = readJson("data/hall.json");

    for (Map<String, Object> m : list) {
      Company company = getCompany(m.get("cmno"));

      HallDetail detail = HallDetail.builder()
          .company(company)
          .hallName((String) m.get("hallName"))
          .address((String) m.get("address"))
          .latitude(toDoubleObject(m.get("latitude")))
          .longitude(toDoubleObject(m.get("longitude")))
          .phone((String) m.get("phone"))
          .representative((String) m.get("representative"))
          .hallType(enumValue(HallType.class, m.get("hallType"), HallType.GRAND))
          .description((String) m.get("description"))
          .imageUrl((String) m.get("imageUrl"))
          .build();
      hallDetailRepository.save(detail);

      List<Map<String, Object>> items = castList(m.get("hallItems"));
      if (items != null) {
        for (Map<String, Object> item : items) {
          HallItem hallItem = HallItem.builder()
              .company(company)
              .itemName((String) item.get("itemName"))
              .price(toBigDecimal(item.get("price")))
              .capacity(toIntegerObject(item.get("capacity")))
              .imageUrl((String) item.get("imageUrl"))
              .ord(toIntegerObject(item.get("ord")))
              .mealType(enumValue(MealType.class, item.get("mealType"), MealType.BUFFET))
              .build();
          hallItemRepository.save(hallItem);
        }
      }
    }

    log.info("Inserted hall details from JSON.");
  }

  private void insertDresses() throws Exception {
    List<Map<String, Object>> list = readJson("data/dress.json");

    for (Map<String, Object> m : list) {
      Company company = getCompany(m.get("cmno"));

      DressDetail detail = DressDetail.builder()
          .company(company)
          .sizeRange((String) m.get("sizeRange"))
          .build();
      dressDetailRepository.save(detail);

      List<Map<String, Object>> items = castList(m.get("dressItems"));
      if (items != null) {
        for (Map<String, Object> item : items) {
          DressItem dressItem = DressItem.builder()
              .company(company)
              .itemName((String) item.get("itemName"))
              .price(toBigDecimal(item.get("price")))
              .imageUrl((String) item.get("imageUrl"))
              .ord(toIntegerObject(item.get("ord")))
              .itemType(normalizeDressItemType(item.get("itemType")))
              .styleTags((String) item.get("styleTags"))
              .sizeRange((String) item.get("sizeRange"))
              .build();
          dressItemRepository.save(dressItem);
        }
      }
    }

    log.info("Inserted dress details from JSON.");
  }

  private void insertStudios() throws Exception {
    List<Map<String, Object>> list = readJson("data/studio.json");

    for (Map<String, Object> m : list) {
      Company company = getCompany(m.get("cmno"));
      List<String> tags = castList(m.get("themeTags"));

      StudioDetail detail = StudioDetail.builder()
          .company(company)
          .themeTags(tags != null ? String.join(",", tags) : "")
          .build();
      studioDetailRepository.save(detail);
    }

    log.info("Inserted studio details from JSON.");
  }

  private void insertMakeups() throws Exception {
    List<Map<String, Object>> list = readJson("data/makeup.json");

    for (Map<String, Object> m : list) {
      Company company = getCompany(m.get("cmno"));

      MakeupDetail detail = MakeupDetail.builder()
          .company(company)
          .includesHairService(toBoolean(m.get("includesHairService")))
          .includesMakeupService(toBooleanDefault(m.get("includesMakeupService"), true))
          .includesNailService(toBoolean(m.get("includesNailService")))
          .hairPrice(toBigDecimalOrNull(m.get("hairPrice")))
          .makeupPrice(toBigDecimalOrNull(m.get("makeupPrice")))
          .nailPrice(toBigDecimalOrNull(m.get("nailPrice")))
          .build();
      makeupDetailRepository.save(detail);

      List<Map<String, Object>> packages = castList(m.get("packages"));
      if (packages != null) {
        for (Map<String, Object> pkg : packages) {
          MakeupPackage makeupPackage = MakeupPackage.builder()
              .company(company)
              .packageType(normalizeMakeupPackageType(pkg.get("packageType")))
              .discountRate(toBigDecimal(pkg.get("discountRate")))
              .build();
          makeupPackageRepository.save(makeupPackage);
        }
      }
    }

    log.info("Inserted makeup details from JSON.");
  }

  private Company getCompany(Object cmno) {
    return companyRepository.findById(toLongObject(cmno))
        .orElseThrow(() -> new IllegalStateException("Company not found for cmno=" + cmno));
  }

  private List<Map<String, Object>> readJson(String path) throws Exception {
    ClassPathResource resource = new ClassPathResource(path);
    try (InputStream is = resource.getInputStream()) {
      return objectMapper.readValue(is, new TypeReference<>() {});
    }
  }

  @SuppressWarnings("unchecked")
  private <T> List<T> castList(Object value) {
    return value instanceof List<?> ? (List<T>) value : null;
  }

  private <E extends Enum<E>> E enumValue(Class<E> enumType, Object value, E defaultValue) {
    if (value == null) {
      return defaultValue;
    }
    try {
      return Enum.valueOf(enumType, value.toString());
    } catch (IllegalArgumentException ex) {
      log.warn("Unknown enum value '{}' for {}. Use default '{}'.", value, enumType.getSimpleName(), defaultValue);
      return defaultValue;
    }
  }

  private DressItemType normalizeDressItemType(Object value) {
    if (value == null) {
      return DressItemType.ALINE;
    }
    String name = value.toString();
    if ("BALL".equals(name)) {
      return DressItemType.BELL;
    }
    return enumValue(DressItemType.class, name, DressItemType.ALINE);
  }

  private MakeupPackageType normalizeMakeupPackageType(Object value) {
    if (value == null) {
      return MakeupPackageType.MAKEUP;
    }
    String name = value.toString();
    if ("HAIR_ONLY".equals(name)) {
      return MakeupPackageType.HAIR;
    }
    if ("MAKEUP_ONLY".equals(name)) {
      return MakeupPackageType.MAKEUP;
    }
    if ("NAIL_MAKEUP".equals(name)) {
      return MakeupPackageType.FULL;
    }
    return enumValue(MakeupPackageType.class, name, MakeupPackageType.MAKEUP);
  }

  private int toInt(Object value) {
    Integer result = toIntegerObject(value);
    return result == null ? 0 : result;
  }

  private Integer toIntegerObject(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Number number) {
      return number.intValue();
    }
    return Integer.parseInt(value.toString());
  }

  private Long toLongObject(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Number number) {
      return number.longValue();
    }
    return Long.parseLong(value.toString());
  }

  private Double toDoubleObject(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Number number) {
      return number.doubleValue();
    }
    return Double.parseDouble(value.toString());
  }

  private BigDecimal toBigDecimal(Object value) {
    BigDecimal result = toBigDecimalOrNull(value);
    return result == null ? BigDecimal.ZERO : result;
  }

  private BigDecimal toBigDecimalOrNull(Object value) {
    if (value == null) {
      return null;
    }
    return new BigDecimal(value.toString());
  }

  private boolean toBoolean(Object value) {
    return toBooleanDefault(value, false);
  }

  private boolean toBooleanDefault(Object value, boolean defaultValue) {
    return value == null ? defaultValue : Boolean.parseBoolean(value.toString());
  }
}
