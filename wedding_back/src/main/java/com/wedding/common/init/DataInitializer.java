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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.wedding.product.domain.Product;
import com.wedding.product.repository.ProductRepository;
import com.wedding.product.domain.ProductOption;
import com.wedding.product.repository.ProductOptionRepository;
import com.wedding.checkout.domain.Orders;
import com.wedding.checkout.domain.OrderItem;
import com.wedding.checkout.repository.OrderRepository;
import com.wedding.member.domain.Member;
import com.wedding.member.domain.MemberDetail;
import com.wedding.member.domain.MemberRole;
import com.wedding.member.repository.MemberDetailRepository;
import com.wedding.member.repository.MemberRepository;

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
  private final ProductRepository productRepository;
  private final ProductOptionRepository productOptionRepository;
  private final OrderRepository orderRepository;
  private final MemberRepository memberRepository;
  private final MemberDetailRepository memberDetailRepository;
  private final PasswordEncoder passwordEncoder;
  private final JdbcTemplate jdbcTemplate;

  @Override
  @Transactional
  public void run(ApplicationArguments args) throws Exception {

    // Product 더미데이터는 company 데이터 유무와 무관하게 독립적으로 체크
    if (productRepository.count() == 0) {
      log.info("===== Product JSON dummy data initialization start =====");
      insertProducts();
      log.info("===== Product JSON dummy data initialization complete =====");
    }

    if (orderRepository.count() == 0) {
      log.info("===== Dummy Orders seeding start =====");
      insertDummyOrders();
      log.info("===== Dummy Orders seeding complete =====");
    }

    // Member 더미데이터도 company 데이터 유무와 무관하게 독립적으로 체크
    // (실계정과 안 겹치도록, 더미 세트의 첫 번째 이메일 존재 여부로 판단)
    if (!memberRepository.existsById("user1@naver.com")) {
      log.info("===== Member JSON dummy data initialization start =====");
      insertMembers();
      log.info("===== Member JSON dummy data initialization complete =====");
    }

    if (companyRepository.count() == 0) {
      log.info("No company data exists. Skip company detail initialization.");
      return;
    }

    relaxLegacyDetailColumns();
    refreshMakeupDiscounts();

  }

  private void relaxLegacyDetailColumns() {
    relaxColumn("tbl_hall_detail", "company_cno");
    relaxColumn("tbl_hall_detail", "capacity");
    relaxColumn("tbl_hall_detail", "meal_type");
    relaxColumn("tbl_hall_detail", "parking_available");
    relaxColumn("tbl_hall_detail", "wedding_time");
    relaxColumn("tbl_dress_detail", "company_cno");
    relaxColumn("tbl_makeup_detail", "company_cno");
    relaxColumn("tbl_studio_detail", "company_cno");
  }

  private void relaxColumn(String tableName, String columnName) {
    List<Map<String, Object>> columns = jdbcTemplate.queryForList("""
        select COLUMN_TYPE, IS_NULLABLE
        from INFORMATION_SCHEMA.COLUMNS
        where TABLE_SCHEMA = DATABASE()
          and TABLE_NAME = ?
          and COLUMN_NAME = ?
        """, tableName, columnName);

    if (columns.isEmpty() || "YES".equals(columns.get(0).get("IS_NULLABLE"))) {
      return;
    }

    String columnType = columns.get(0).get("COLUMN_TYPE").toString();
    jdbcTemplate.execute("alter table `" + tableName + "` modify column `" + columnName + "` " + columnType + " null");
    log.info("Relaxed legacy column {}.{} to nullable.", tableName, columnName);
  }

  private void refreshMakeupDiscounts() throws Exception {
    log.info("===== Makeup discount JSON synchronization start =====");

    makeupPackageRepository.deleteAll();

    insertMakeupDiscountPackages();

    log.info("===== Makeup discount JSON synchronization complete =====");
  }

  private void insertMakeupDiscountPackages() throws Exception {
    List<Map<String, Object>> list = readJson("data/makeup.json");

    for (Map<String, Object> m : list) {
      Company company = findCompany(m.get("cmno"));
      if (company == null) {
        continue;
      }

      List<Map<String, Object>> packages = castList(m.get("packages"));
      if (packages == null) {
        continue;
      }

      for (Map<String, Object> pkg : packages) {
        if (pkg == null) {
          continue;
        }
        MakeupPackage makeupPackage = MakeupPackage.builder()
                .company(company)
                .packageType(normalizeMakeupPackageType(pkg.get("packageType")))
                .discountRate(toBigDecimal(pkg.get("discountRate")))
                .build();
        makeupPackageRepository.save(makeupPackage);
      }
    }

    log.info("Inserted makeup discount packages from JSON.");
  }

  // 상품 더미데이터 삽입 (옵션 포함)
  private void insertProducts() throws Exception {
    List<Map<String, Object>> list = readJson("data/product.json");

    for (Map<String, Object> m : list) {
      Product product = Product.builder()
              .pname((String) m.get("pname"))
              .price(toInt(m.get("price")))
              .pdesc((String) m.get("pdesc"))
              .category((String) m.get("category"))
              .stockQty(toInt(m.get("stockQty")))
              .build();

      List<String> uploadFileNames = castList(m.get("uploadFileNames"));
      if (uploadFileNames != null) {
        uploadFileNames.forEach(product::addImageString);
      }

      Product savedProduct = productRepository.save(product);

      List<Map<String, Object>> options = castList(m.get("options"));
      if (options != null) {
        for (Map<String, Object> opt : options) {
          ProductOption productOption = ProductOption.builder()
                  .product(savedProduct)
                  .optionName((String) opt.get("optionName"))
                  .optionValue((String) opt.get("optionValue"))
                  .extraPrice(toInt(opt.get("extraPrice")))
                  .build();

          productOptionRepository.save(productOption);
        }
      }
    }

    log.info("Inserted {} products from JSON.", productRepository.count());
  }

  // 리뷰 테스트를 위해, 실제 존재하는 회원들에게 상품 몇 개를 "구매한 것"으로 만들어줌
  private void insertDummyOrders() {

    List<Member> members = memberRepository.findAll();

    if (members.isEmpty()) {
      log.info("회원이 없어 더미 주문 생성을 건너뜁니다.");
      return;
    }

    createDummyOrdersForMembers(members, 3);
  }

  // 주어진 회원들에게 상품 productCount개씩 "구매완료" 주문을 만들어줌 (리뷰 테스트용)
  private void createDummyOrdersForMembers(List<Member> members, int productCount) {

    List<Product> products = productRepository.findAll();

    if (members.isEmpty() || products.isEmpty()) {
      log.info("회원 또는 상품이 없어 더미 주문 생성을 건너뜁니다.");
      return;
    }

    int count = Math.min(productCount, products.size());

    for (Member member : members) {
      for (int i = 0; i < count; i++) {
        Product product = products.get(i);

        Orders orders = Orders.builder()
                .orderNumber("DUMMY-" + member.getEmail() + "-" + product.getPno())
                .member(member)
                .totalPrice(product.getPrice())
                .shippingFee(0)
                .receiverName(member.getNickname())
                .receiverPhone("010-0000-0000")
                .address("테스트 주소")
                .orderStatus("DELIVERED")
                .build();

        OrderItem orderItem = OrderItem.builder()
                .product(product)
                .pnameSnapshot(product.getPname())
                .priceSnapshot(product.getPrice())
                .qty(1)
                .build();

        orders.addOrderItem(orderItem);

        orderRepository.save(orders);
      }
    }

    log.info("Inserted dummy orders for {} members x {} products.", members.size(), count);
  }

  // 회원 더미데이터 삽입 (MemberDetail 포함) + 그 중 절반은 상품 2개씩 구매한 것으로 처리
  private void insertMembers() throws Exception {

    List<Map<String, Object>> list = readJson("data/member.json");

    List<Member> insertedMembers = new ArrayList<>();

    for (Map<String, Object> m : list) {

      String email = (String) m.get("email");

      // 혹시 이미 있는 이메일(테스트 계정 등)이면 건드리지 않고 넘어감
      if (memberRepository.existsById(email)) {
        continue;
      }

      Member member = Member.builder()
              .email(email)
              .pw(passwordEncoder.encode("1111"))   // 더미 계정 공통 비밀번호
              .nickname((String) m.get("nickname"))
              .social(Boolean.TRUE.equals(m.get("social")))
              .status((String) m.getOrDefault("status", "ACTIVE"))
              .emailVerified(Boolean.TRUE.equals(m.get("emailVerified")))
              .suspendReason((String) m.get("suspendReason"))
              .suspendUntil(parseDateTime((String) m.get("suspendUntil")))
              .lastLoginAt(parseDateTime((String) m.get("lastLoginAt")))
              .build();

      List<String> roleNames = castList(m.get("roleNames"));

      if (roleNames != null && roleNames.contains("ADMIN")) {
        member.addRole(MemberRole.ADMIN);
      } else {
        member.addRole(MemberRole.USER);
      }

      memberRepository.save(member);

      MemberDetail detail = MemberDetail.builder()
              .member(member)
              .name((String) m.get("name"))
              .phone((String) m.get("phone"))
              .build();

      memberDetailRepository.save(detail);

      insertedMembers.add(member);
    }

    log.info("Inserted {} dummy members from JSON (password: 1111).", insertedMembers.size());

    // 100명 중 절반(앞쪽 50명)만 상품 2개씩 구매완료 처리 -> 리뷰 테스트용
    if (!insertedMembers.isEmpty()) {
      int half = insertedMembers.size() / 2;
      List<Member> orderTargets = insertedMembers.subList(0, half);

      log.info("===== Dummy orders for {} newly seeded members (2 products each) start =====", orderTargets.size());
      createDummyOrdersForMembers(orderTargets, 2);
      log.info("===== Dummy orders for newly seeded members complete =====");
    }
  }

  private LocalDateTime parseDateTime(String value) {
    return (value == null || value.isBlank()) ? null : LocalDateTime.parse(value);
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
      Company company = findCompany(m.get("cmno"));
      if (company == null) {
        continue;
      }

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
      Company company = findCompany(m.get("cmno"));
      if (company == null) {
        continue;
      }

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
      Company company = findCompany(m.get("cmno"));
      if (company == null) {
        continue;
      }
      List<String> tags = castList(m.get("themeTags"));
      String themeTags = tags != null ? String.join(",", tags) : (String) m.getOrDefault("theme", "");

      StudioDetail detail = StudioDetail.builder()
              .company(company)
              .themeTags(themeTags)
              .build();
      studioDetailRepository.save(detail);
    }

    log.info("Inserted studio details from JSON.");
  }

  private void insertMakeups() throws Exception {
    List<Map<String, Object>> list = readJson("data/makeup.json");

    for (Map<String, Object> m : list) {
      Company company = findCompany(m.get("cmno"));
      if (company == null) {
        continue;
      }

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
          if (pkg == null) {
            continue;
          }
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

  private Company findCompany(Object cmno) {
    Long companyId = toLongObject(cmno);
    return companyRepository.findById(companyId)
            .orElseGet(() -> {
              log.warn("Skip detail data. Company not found for cmno={}", companyId);
              return null;
            });
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
    if ("TWO".equals(name)) {
      return MakeupPackageType.HAIR_MAKEUP;
    }
    if ("THREE".equals(name)) {
      return MakeupPackageType.FULL;
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