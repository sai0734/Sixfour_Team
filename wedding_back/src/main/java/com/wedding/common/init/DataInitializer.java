package com.wedding.common.init;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedding.board.domain.Board;
import com.wedding.board.domain.Comment;
import com.wedding.board.repository.BoardRepository;
import com.wedding.board.repository.CommentRepository;
import com.wedding.checkout.domain.OrderItem;
import com.wedding.checkout.domain.Orders;
import com.wedding.checkout.repository.OrderRepository;
import com.wedding.company.domain.Company;
import com.wedding.company.domain.CompanyPackage;
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
import com.wedding.company.repository.CompanyPackageRepository;
import com.wedding.company.repository.CompanyRepository;
import com.wedding.company.repository.DressDetailRepository;
import com.wedding.company.repository.DressItemRepository;
import com.wedding.company.repository.HallDetailRepository;
import com.wedding.company.repository.HallItemRepository;
import com.wedding.company.repository.MakeupDetailRepository;
import com.wedding.company.repository.MakeupPackageRepository;
import com.wedding.company.repository.StudioDetailRepository;
import com.wedding.faq.domain.Faq;
import com.wedding.faq.repository.FaqRepository;
import com.wedding.member.domain.Member;
import com.wedding.member.domain.MemberDetail;
import com.wedding.member.domain.MemberRole;
import com.wedding.member.repository.MemberDetailRepository;
import com.wedding.member.repository.MemberRepository;
import com.wedding.product.domain.Product;
import com.wedding.product.domain.ProductOption;
import com.wedding.product.domain.Qna;
import com.wedding.product.domain.Review;
import com.wedding.product.repository.ProductOptionRepository;
import com.wedding.product.repository.ProductRepository;
import com.wedding.product.repository.QnaRepository;
import com.wedding.product.repository.ReviewRepository;
// 재원 추가 - 업체 상세페이지 "결제 완료 N건" 인기도 지표가 서비스 초기부터 자연스럽게 보이도록
// 업체별 더미 결제완료(PAID) 예약을 심어두기 위해 가져옴
import com.wedding.reservation.domain.Reservation;
import com.wedding.reservation.repository.ReservationRepository;
// 재원 추가 끝
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Log4j2
public class DataInitializer implements ApplicationRunner {

  private static final String ADMIN_EMAIL = "user1@naver.com";

  private final CompanyRepository companyRepository;
  private final CompanyPackageRepository companyPackageRepository;
  private final MakeupPackageRepository makeupPackageRepository;
  private final HallDetailRepository hallDetailRepository;
  private final HallItemRepository hallItemRepository;
  private final DressDetailRepository dressDetailRepository;
  private final DressItemRepository dressItemRepository;
  private final StudioDetailRepository studioDetailRepository;
  private final MakeupDetailRepository makeupDetailRepository;
  private final ObjectMapper objectMapper;
  private final ProductRepository productRepository;
  private final ProductOptionRepository productOptionRepository;
  private final OrderRepository orderRepository;
  private final ReviewRepository reviewRepository;
  private final QnaRepository qnaRepository;
  private final MemberRepository memberRepository;
  private final MemberDetailRepository memberDetailRepository;
  private final PasswordEncoder passwordEncoder;
  private final JdbcTemplate jdbcTemplate;
  private final BoardRepository boardRepository;
  private final CommentRepository commentRepository;
  // 재원 추가 - 업체별 더미 결제완료(PAID) 예약 시딩용
  private final ReservationRepository reservationRepository;
  // insertCompanies()에서 company.json의 purchaseCount를 읽어 담아두고, insertReservationPurchaseDummy()에서 사용
  private Map<Long, Integer> companyPurchaseCountMap;
  // 재원 추가 끝
  // 재원 수정 - FAQ 더미데이터를 faq/config/FaqDummyDataLoader.java(별도 CommandLineRunner,
  // 하드코딩된 Java 데이터)에서 다른 도메인들과 동일한 방식(data/*.json + DataInitializer)으로 통일
  private final FaqRepository faqRepository;

  // 애플리케이션 기동 시 더미 시드 진입점 (모든 시드: 해당 테이블 count == 0 일 때만)
  // 1) data/product.json         → tbl_product, tbl_product_option
  // 2) data/member.json          → tbl_member, tbl_member_detail
  // 3) data/board.json           → tbl_board (작성자는 ACTIVE 회원 순환 배정)
  // 4) data/comment.json         → tbl_comment (작성자는 ACTIVE 회원 순환 배정, board 이후)
  // 5) data/commerce_dummy.json  → tbl_orders, tbl_order_item, tbl_review, tbl_qna
  // 6) data/faq.json             → tbl_faq
  // 7) data/makeup.json          → tbl_makeup_package (업체 DB 있을 때만)
  // 8) data/companyPackage.json  → tbl_company_package (업체 DB 신규 삽입 시에만)
  @Override
  @Transactional
  public void run(ApplicationArguments args) throws Exception {

    if (productRepository.count() == 0) {
      log.info("===== Product dummy seed start =====");
      insertProducts();
      log.info("===== Product dummy seed complete =====");
    }

    if (memberRepository.count() == 0) {
      log.info("===== Member dummy seed start =====");
      insertMembers();
      log.info("===== Member dummy seed complete =====");
    }

    // 게시판/댓글은 반드시 회원 삽입 이후에 실행되어야 함 (작성자를 실제 회원으로 배정하므로)
    if (boardRepository.count() == 0) {
      log.info("===== Board dummy seed start =====");
      insertBoards();
      log.info("===== Board dummy seed complete =====");
    }

    if (commentRepository.count() == 0) {
      log.info("===== Comment dummy seed start =====");
      insertComments();
      log.info("===== Comment dummy seed complete =====");
    }

    // 재원 추가 - FAQ는 다른 데이터와 의존관계 없어서 순서 상관없이 아무데나 둬도 됨
    if (faqRepository.count() == 0) {
      log.info("===== Faq dummy seed start =====");
      insertFaqs();
      log.info("===== Faq dummy seed complete =====");
    }

    if (orderRepository.count() == 0) {
      log.info("===== Commerce dummy seed start =====");
      insertCommerceDummyFromJson();
      log.info("===== Commerce dummy seed complete =====");
    }

    if (companyRepository.count() == 0) {
      log.info("===== Company dummy seed start =====");
      Map<Long, Company> companyMap = insertCompanies();
      log.info("===== Company dummy seed complete. count={} =====", companyRepository.count());
      if (!companyMap.isEmpty()) {
        log.info("===== Company detail dummy seed start =====");
        insertHallDetails(companyMap);
        insertDressDetails(companyMap);
        insertStudioDetails(companyMap);
        insertMakeupDetails(companyMap);
        log.info("===== Company detail dummy seed complete =====");

        log.info("===== Company package dummy seed start =====");
        insertCompanyPackages(companyMap);
        log.info("===== Company package dummy seed complete. count={} =====", companyPackageRepository.count());

        // 재원 추가 - 업체별 "결제 완료 N건" 더미 시딩 (패키지의 purchaseCount와는 별개로,
        // 개별 업체 상세페이지에서 쓰는 실제 카운트 쿼리(getPaymentCount)에 기준치를 만들어줌)
        log.info("===== Company purchase-count dummy seed start =====");
        insertReservationPurchaseDummy(companyMap);
        log.info("===== Company purchase-count dummy seed complete =====");
        // 재원 추가 끝
      }
    } else {
      relaxLegacyDetailColumns();
      // 재원 추가 - normalizeMakeupPackageType() 정규화 로직이 생기기 전에 이미 DB에 들어간
      // 레거시 packageType 값("TWO"/"THREE" 등)은 count()==0 조건에 안 걸려서 영영 안 고쳐짐.
      // 서버 재시작할 때마다 자동으로 정리되도록 매번 실행 (이미 정상 값이면 그냥 0건 업데이트, 무해함)
      normalizeLegacyMakeupPackageTypes();
      if (makeupPackageRepository.count() == 0) {
        log.info("===== Makeup package dummy seed start =====");
        insertMakeupDiscountPackages();
        log.info("===== Makeup package dummy seed complete =====");
      }
    }
  }

  // 업체 상세 테이블 legacy NOT NULL 컬럼 보정 (더미 삽입 아님, INFORMATION_SCHEMA 조회)
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

  // 재원 추가 - tbl_makeup_package.package_type에 남아있는 레거시 값("TWO"/"THREE" 등)을
  // 현재 MakeupPackageType enum 표준값(HAIR_MAKEUP/FULL 등)으로 자동 정규화.
  // normalizeMakeupPackageType()은 "새로 시드할 때"만 적용되는 로직이라, 그보다 먼저
  // DB에 들어간 레거시 값은 이걸로 매 기동 시마다 정리해줌 (이미 정상이면 0건 업데이트, 무해함).
  private void normalizeLegacyMakeupPackageTypes() {
    Map<String, String> legacyToStandard = new HashMap<>();
    legacyToStandard.put("TWO", "HAIR_MAKEUP");
    legacyToStandard.put("THREE", "FULL");
    legacyToStandard.put("NAIL_MAKEUP", "FULL");
    legacyToStandard.put("HAIR_ONLY", "HAIR");
    legacyToStandard.put("MAKEUP_ONLY", "MAKEUP");

    legacyToStandard.forEach((legacy, standard) -> {
      int updated = jdbcTemplate.update(
              "update tbl_makeup_package set package_type = ? where package_type = ?",
              standard, legacy);
      if (updated > 0) {
        log.info("Normalized {} legacy makeup package_type '{}' -> '{}'.", updated, legacy, standard);
      }
    });
  }

  // 단일 컬럼 nullable 변경 헬퍼 (더미 데이터 없음)
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

  // data/company.json → tbl_company + tbl_company_image. 반환: jsonCmno → 저장된 Company 맵
  private Map<Long, Company> insertCompanies() throws Exception {
    List<Map<String, Object>> list = readJsonArray("data/company.json");
    Map<Long, Company> companyMap = new HashMap<>();
    // 재원 추가 - company.json의 purchaseCount를 같이 읽어서, 나중에 더미 결제 건수 시딩에 재사용
    companyPurchaseCountMap = new HashMap<>();
    // 재원 추가 끝

    for (Map<String, Object> m : list) {
      Long jsonCmno = toLongObject(m.get("cmno"));

      Company company = Company.builder()
              .category(enumValue(com.wedding.company.domain.CompanyCategory.class, m.get("category"), null))
              .name((String) m.get("name"))
              .ceoName((String) m.get("ceoName"))
              .phone((String) m.get("phone"))
              .address((String) m.get("address"))
              .latitude(toDouble(m.get("latitude")))
              .longitude(toDouble(m.get("longitude")))
              .description((String) m.get("description"))
              .priceAvg(toBigDecimal(m.get("priceAvg")))
              .build();

      List<Map<String, Object>> imageList = castList(m.get("imageList"));
      if (imageList != null) {
        for (Map<String, Object> img : imageList) {
          company.addImage((String) img.get("fileName"));
        }
      }

      Company saved = companyRepository.save(company);
      if (jsonCmno != null) {
        companyMap.put(jsonCmno, saved);
        // 재원 추가 - purchaseCount 보관 (없으면 0)
        companyPurchaseCountMap.put(
                saved.getCmno(), toIntegerObject(m.get("purchaseCount")) == null ? 0 : toIntegerObject(m.get("purchaseCount")));
        // 재원 추가 끝
      }
    }

    return companyMap;
  }

  // data/hall.json → tbl_hall_detail + tbl_hall_item
  private void insertHallDetails(Map<Long, Company> companyMap) throws Exception {
    List<Map<String, Object>> list = readJsonArray("data/hall.json");

    for (Map<String, Object> m : list) {
      Company company = companyMap.get(toLongObject(m.get("cmno")));
      if (company == null) {
        log.warn("Skip hall detail. Company not found for cmno={}", m.get("cmno"));
        continue;
      }

      hallDetailRepository.save(HallDetail.builder()
              .company(company)
              .hallName((String) m.get("hallName"))
              .address((String) m.get("address"))
              .latitude(toDouble(m.get("latitude")))
              .longitude(toDouble(m.get("longitude")))
              .phone((String) m.get("phone"))
              .representative((String) m.get("representative"))
              .hallType(enumValue(HallType.class, m.get("hallType"), null))
              .description((String) m.get("description"))
              .imageUrl((String) m.get("imageUrl"))
              .build());

      List<Map<String, Object>> items = castList(m.get("hallItems"));
      if (items != null) {
        for (Map<String, Object> item : items) {
          hallItemRepository.save(HallItem.builder()
                  .company(company)
                  .itemName((String) item.get("itemName"))
                  .price(toBigDecimal(item.get("price")))
                  .capacity(toIntegerObject(item.get("capacity")))
                  .imageUrl((String) item.get("imageUrl"))
                  .ord(toIntegerObject(item.get("ord")))
                  .mealType(enumValue(MealType.class, item.get("mealType"), null))
                  .build());
        }
      }
    }

    log.info("Inserted {} hall details, {} hall items.", hallDetailRepository.count(), hallItemRepository.count());
  }

  // data/dress.json → tbl_dress_detail + tbl_dress_item
  private void insertDressDetails(Map<Long, Company> companyMap) throws Exception {
    List<Map<String, Object>> list = readJsonArray("data/dress.json");

    for (Map<String, Object> m : list) {
      Company company = companyMap.get(toLongObject(m.get("cmno")));
      if (company == null) {
        log.warn("Skip dress detail. Company not found for cmno={}", m.get("cmno"));
        continue;
      }

      dressDetailRepository.save(DressDetail.builder()
              .company(company)
              .sizeRange((String) m.get("sizeRange"))
              .build());

      List<Map<String, Object>> items = castList(m.get("dressItems"));
      if (items != null) {
        for (Map<String, Object> item : items) {
          dressItemRepository.save(DressItem.builder()
                  .company(company)
                  .itemName((String) item.get("itemName"))
                  .price(toBigDecimal(item.get("price")))
                  .imageUrl((String) item.get("imageUrl"))
                  .ord(toIntegerObject(item.get("ord")))
                  .itemType(enumValue(DressItemType.class, item.get("itemType"), null))
                  .styleTags((String) item.get("styleTags"))
                  .sizeRange((String) item.get("sizeRange"))
                  .build());
        }
      }
    }

    log.info("Inserted {} dress details, {} dress items.", dressDetailRepository.count(), dressItemRepository.count());
  }

  // data/studio.json → tbl_studio_detail
  private void insertStudioDetails(Map<Long, Company> companyMap) throws Exception {
    List<Map<String, Object>> list = readJsonArray("data/studio.json");

    for (Map<String, Object> m : list) {
      Company company = companyMap.get(toLongObject(m.get("cmno")));
      if (company == null) {
        log.warn("Skip studio detail. Company not found for cmno={}", m.get("cmno"));
        continue;
      }

      List<String> themeTagList = castList(m.get("themeTags"));
      String themeTags = themeTagList != null
              ? String.join(",", themeTagList)
              : (String) m.get("themeTags");

      studioDetailRepository.save(StudioDetail.builder()
              .company(company)
              .themeTags(themeTags)
              .build());
    }

    log.info("Inserted {} studio details.", studioDetailRepository.count());
  }

  // data/makeup.json → tbl_makeup_detail + tbl_makeup_package (신규 DB용)
  private void insertMakeupDetails(Map<Long, Company> companyMap) throws Exception {
    List<Map<String, Object>> list = readJsonArray("data/makeup.json");

    for (Map<String, Object> m : list) {
      Company company = companyMap.get(toLongObject(m.get("cmno")));
      if (company == null) {
        log.warn("Skip makeup detail. Company not found for cmno={}", m.get("cmno"));
        continue;
      }

      MakeupDetail detail = MakeupDetail.builder().company(company).build();
      detail.change(
              Boolean.TRUE.equals(m.get("includesHairService")),
              Boolean.TRUE.equals(m.get("includesMakeupService")),
              Boolean.TRUE.equals(m.get("includesNailService")),
              toBigDecimalNullable(m.get("hairPrice")),
              toBigDecimalNullable(m.get("makeupPrice")),
              toBigDecimalNullable(m.get("nailPrice"))
      );
      makeupDetailRepository.save(detail);

      List<Map<String, Object>> packages = castList(m.get("packages"));
      if (packages != null) {
        for (Map<String, Object> pkg : packages) {
          if (pkg == null) continue;
          makeupPackageRepository.save(MakeupPackage.builder()
                  .company(company)
                  .packageType(normalizeMakeupPackageType(pkg.get("packageType")))
                  .discountRate(toBigDecimal(pkg.get("discountRate")))
                  .build());
        }
      }
    }

    log.info("Inserted {} makeup details, {} makeup packages.",
            makeupDetailRepository.count(), makeupPackageRepository.count());
  }

  // data/companyPackage.json → tbl_company_package
  // 홀+드레스+스튜디오+메이크업(스드메) 4개 업체를 묶은 패키지 더미 시드.
  // insertCompanies()가 만든 companyMap(jsonCmno → 저장된 Company)을 그대로 재사용함.
  private void insertCompanyPackages(Map<Long, Company> companyMap) throws Exception {
    List<Map<String, Object>> list = readJsonArray("data/companyPackage.json");

    for (Map<String, Object> m : list) {
      Company hallCompany = companyMap.get(toLongObject(m.get("hallCmno")));
      Company dressCompany = companyMap.get(toLongObject(m.get("dressCmno")));
      Company studioCompany = companyMap.get(toLongObject(m.get("studioCmno")));
      Company makeupCompany = companyMap.get(toLongObject(m.get("makeupCmno")));

      if (hallCompany == null || dressCompany == null || studioCompany == null || makeupCompany == null) {
        log.warn("Skip company package '{}'. referenced company not found in seed map.", m.get("name"));
        continue;
      }

      companyPackageRepository.save(CompanyPackage.builder()
              .name((String) m.get("name"))
              .description((String) m.get("description"))
              .hallCompany(hallCompany)
              .dressCompany(dressCompany)
              .studioCompany(studioCompany)
              .makeupCompany(makeupCompany)
              .totalPrice(toBigDecimal(m.get("totalPrice")))
              .discountRate(m.get("discountRate") == null ? null : ((Number) m.get("discountRate")).doubleValue())
              .packagePrice(toBigDecimal(m.get("packagePrice")))
              .distanceKm(m.get("distanceKm") == null ? null : ((Number) m.get("distanceKm")).doubleValue())
              .purchaseCount(((Number) m.getOrDefault("purchaseCount", 0)).intValue())
              .build());
    }
  }

  // 재원 추가 - 업체별 "결제 완료 N건" 더미 시딩.
  // getPaymentCount(cmno)는 tbl_reservation에서 payStatus='PAID'인 실제 행 개수를 그대로 세기
  // 때문에, 미리 보여줄 기준치를 만들려면 결제 로직(preparePayment/confirmPayment)을 거치지 않고
  // 바로 PAID 상태인 예약 행을 심어두는 방법뿐임. 개수는 data/company.json의 purchaseCount 값을
  // 그대로 따름 (다른 더미데이터와 동일하게 JSON에서 관리 - Java에 값을 하드코딩하지 않음).
  // optionName/weddingDate 값 자체는 집계에 쓰이지 않으므로 의미 없는 고정값으로 둠.
  // (CompanyPackage.purchaseCount는 패키지 자체의 더미 지표라 이것과는 별개)
  // 이후 실제 사용자가 결제를 완료하면 이 기준치 위에 자연스럽게 누적됨.
  private void insertReservationPurchaseDummy(Map<Long, Company> companyMap) {
    if (reservationRepository.count() > 0) {
      return;
    }

    LocalDate baseDate = LocalDate.now().minusMonths(6);
    int i = 0;

    for (Company company : companyMap.values()) {
      int purchaseCount = companyPurchaseCountMap.getOrDefault(company.getCmno(), 0);

      for (int n = 0; n < purchaseCount; n++) {
        Reservation dummy = Reservation.builder()
                .cmno(company.getCmno())
                .memberEmail("dummy" + (n % 20) + "@wedding.demo")
                .weddingDate(baseDate.plusDays((i * 3) % 180))
                .status("확정")
                .optionName("-")
                .amount(company.getPriceAvg() != null ? company.getPriceAvg().intValue() : 100000)
                .payStatus("PAID")
                .build();
        reservationRepository.save(dummy);
        i++;
      }
    }

    log.info("Inserted {} dummy PAID reservations for company purchase-count seeding.",
            reservationRepository.count());
  }
  // 재원 추가 끝

  private void insertMakeupDiscountPackages() throws Exception {
    List<Map<String, Object>> list = readJsonArray("data/makeup.json");

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

    log.info("Inserted {} makeup discount packages.", makeupPackageRepository.count());
  }

  // data/product.json (options, uploadFileNames 포함) → tbl_product, tbl_product_option
  private void insertProducts() throws Exception {
    List<Map<String, Object>> list = readJsonArray("data/product.json");

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

    log.info("Inserted {} products.", productRepository.count());
  }

  // data/member.json → tbl_member, tbl_member_detail (비밀번호 공통 1111)
  private void insertMembers() throws Exception {
    List<Map<String, Object>> list = readJsonArray("data/member.json");

    for (Map<String, Object> m : list) {
      Member member = Member.builder()
              .email((String) m.get("email"))
              .pw(passwordEncoder.encode("1111"))
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
      }
      member.addRole(MemberRole.USER);

      memberRepository.save(member);

      MemberDetail detail = MemberDetail.builder()
              .member(member)
              .name((String) m.get("name"))
              .phone((String) m.get("phone"))
              .build();
      memberDetailRepository.save(detail);
    }

    log.info("Inserted {} members (password: 1111).", memberRepository.count());
  }

  // data/board.json → tbl_board. 작성자는 고정 가짜 이메일이 아니라 실제 ACTIVE 회원을 순환 배정
  private void insertBoards() throws Exception {
    List<Map<String, Object>> list = readJsonArray("data/board.json");

    List<Member> authors = getActiveMembers();
    if (authors.isEmpty()) {
      log.warn("ACTIVE 상태 회원이 없어 게시판 더미 시드를 건너뜁니다.");
      return;
    }

    List<Board> boards = new ArrayList<>();
    int i = 0;

    for (Map<String, Object> m : list) {
      Member author = authors.get(i % authors.size());

      int viewCount = 80 + (i * 37) % 900;
      int likeCount = 5 + (i * 7) % 90;
      long daysAgo = i % 45;
      long hoursAgo = (i * 3) % 24;

      boards.add(Board.builder()
              .memberEmail(author.getEmail())
              .nickname(author.getNickname())
              .boardType((String) m.get("boardType"))
              .category((String) m.get("category"))
              .title((String) m.get("title"))
              .content((String) m.get("content"))
              .rating(toIntegerObject(m.get("rating")))
              .viewCount(viewCount)
              .likeCount(likeCount)
              .regDate(LocalDateTime.now().minusDays(daysAgo).minusHours(hoursAgo))
              .build());

      i++;
    }

    boardRepository.saveAll(boards);

    log.info("Inserted {} boards (authors: real ACTIVE members).", boards.size());
  }

  // data/comment.json → tbl_comment. board.json과 동일하게 ACTIVE 회원을 순환 배정 (offset 7)
  private void insertComments() throws Exception {
    List<Map<String, Object>> list = readJsonArray("data/comment.json");

    List<Member> authors = getActiveMembers();
    if (authors.isEmpty()) {
      log.warn("ACTIVE 상태 회원이 없어 댓글 더미 시드를 건너뜁니다.");
      return;
    }

    int i = 0;

    for (Map<String, Object> m : list) {
      String boardTitle = (String) m.get("boardTitle");
      String content = (String) m.get("content");
      String reply = (String) m.get("reply");

      Long parentId = addComment(boardTitle, content, authors, i++, null);

      if (reply != null && parentId != null) {
        addComment(boardTitle, reply, authors, i++, parentId);
      }
    }

    log.info("Inserted {} comments (authors: real ACTIVE members).", commentRepository.count());
  }

  // 댓글/대댓글 1건 저장 + 해당 게시글 commentCount 증가
  private Long addComment(String boardTitle, String content, List<Member> authors, int i, Long parentId) {
    Optional<Board> result = boardRepository.findFirstByTitle(boardTitle);

    if (result.isEmpty()) {
      log.warn("댓글 더미 대상 게시글을 못 찾음: {}", boardTitle);
      return null;
    }

    Board board = result.get();

    // 게시글 작성자 배정(offset 0)과 겹치지 않도록 offset 7
    Member author = authors.get((i + 7) % authors.size());

    Comment saved = commentRepository.save(Comment.builder()
            .boardId(board.getBoardId())
            .memberEmail(author.getEmail())
            .nickname(author.getNickname())
            .parentId(parentId)
            .content(content)
            .regDate(LocalDateTime.now().minusDays(i % 20).minusHours((i * 2) % 24))
            .build());

    board.increaseCommentCount();
    boardRepository.save(board);

    return saved.getCommentId();
  }

  // ACTIVE 상태 회원만 게시판/댓글 더미 작성자 후보로 사용 (정지·휴면 계정 제외)
  private List<Member> getActiveMembers() {
    return memberRepository.findAll().stream()
            .filter(member -> "ACTIVE".equals(member.getStatus()))
            .sorted(Comparator.comparing(Member::getEmail))
            .toList();
  }

  // 재원 추가 - data/faq.json → tbl_faq
  // (원래 faq/config/FaqDummyDataLoader.java에 Java 코드로 하드코딩되어 있던 것을
  //  다른 도메인들과 동일하게 JSON + DataInitializer 방식으로 통일함. 회원 등 다른 데이터에
  //  의존하지 않는 단순 목록이라 별다른 가공 없이 그대로 저장)
  private void insertFaqs() throws Exception {
    List<Map<String, Object>> list = readJsonArray("data/faq.json");

    List<Faq> faqs = new ArrayList<>();
    for (Map<String, Object> m : list) {
      faqs.add(Faq.builder()
              .category((String) m.get("category"))
              .sortOrder(toInt(m.get("sortOrder")))
              .likeCount(toInt(m.get("likeCount")))
              .question((String) m.get("question"))
              .answer((String) m.get("answer"))
              .build());
    }

    faqRepository.saveAll(faqs);
    log.info("Inserted {} faqs.", faqs.size());
  }

  // data/commerce_dummy.json → tbl_orders, tbl_order_item, tbl_review, tbl_qna
  private void insertCommerceDummyFromJson() throws Exception {
    Map<String, Object> root = readJsonObject("data/commerce_dummy.json");

    @SuppressWarnings("unchecked")
    Map<String, Object> meta = (Map<String, Object>) root.get("meta");
    String adminEmail = meta != null && meta.get("adminEmail") != null
            ? meta.get("adminEmail").toString()
            : ADMIN_EMAIL;
    Member admin = memberRepository.findById(adminEmail).orElse(null);

    Map<String, OrderItem> orderItemMap = new HashMap<>();

    List<Map<String, Object>> orders = castList(root.get("orders"));
    if (orders != null) {
      for (Map<String, Object> o : orders) {
        saveOrderFromJson(o, orderItemMap);
      }
      spreadOrderRegDates();
      spreadOrderStatuses();
    }

    List<Map<String, Object>> reviews = castList(root.get("reviews"));
    if (reviews != null) {
      for (Map<String, Object> r : reviews) {
        saveReviewFromJson(r, orderItemMap, admin);
      }
    }

    List<Map<String, Object>> qnas = castList(root.get("qnas"));
    if (qnas != null) {
      for (Map<String, Object> q : qnas) {
        saveQnaFromJson(q, admin);
      }
    }

    refreshAllProductRatingStats(productRepository.findAll());

    log.info("Commerce dummy seeded. orders={}, reviews={}, qnas={}",
            orderRepository.count(), reviewRepository.count(), qnaRepository.count());
  }

  // commerce_dummy.json 주문은 JPA Auditing(@CreatedDate) 때문에 전부 "기동 시각"으로 regdate가
  // 찍혀서, 월별 매출 추이 대시보드가 이번 달에만 몰리고 나머지 달은 텅 비어버림.
  // 그래서 삽입 직후 raw SQL로 최근 6개월(이번 달 포함)에 걸쳐 무작위로 재분배함.
  // 최근 달일수록 주문이 더 많도록 가중치를 둬서(성장 추세) 실제 서비스처럼 보이게 함.
  private void spreadOrderRegDates() {
    List<Long> orderIds = jdbcTemplate.queryForList("select ono from tbl_orders", Long.class);
    if (orderIds.isEmpty()) {
      return;
    }

    int monthsBack = 5; // 이번 달 포함 총 6개월
    YearMonth currentMonth = YearMonth.now();
    List<YearMonth> months = new ArrayList<>();
    for (int i = monthsBack; i >= 0; i--) {
      months.add(currentMonth.minusMonths(i));
    }

    // 오래된 달일수록 가중치 낮게, 최근 달일수록 높게 (1,2,3,4,5,6)
    List<Integer> weights = new ArrayList<>();
    for (int i = 1; i <= months.size(); i++) {
      weights.add(i);
    }
    int totalWeight = weights.stream().mapToInt(Integer::intValue).sum();

    Random random = new Random();

    for (Long ono : orderIds) {
      YearMonth month = pickWeightedMonth(months, weights, totalWeight, random);
      LocalDateTime regDate = randomDateTimeInMonth(month, random);

      jdbcTemplate.update("update tbl_orders set regdate = ? where ono = ?", regDate, ono);
    }

    log.info("Spread {} order regdate values across {} months ({} ~ {}).",
            orderIds.size(), months.size(), months.get(0), months.get(months.size() - 1));
  }

  // commerce_dummy.json 주문은 전부 orderStatus가 PAID/SHIPPING/DELIVERED 셋 중 하나로만 채워져 있어서
  // 관리자 대시보드 "처리 필요 주문" 카드(결제완료/배송준비/교환신청/환불신청 4분류)가 결제완료 쪽으로만
  // 쏠려 보임. 그래서 PAID 주문 일부를 배송준비/교환신청/환불신청으로 재분배해 실제 서비스처럼 고르게 보이게 함.
  private void spreadOrderStatuses() {
    List<Long> paidOrderIds = jdbcTemplate.queryForList(
            "select ono from tbl_orders where order_status = 'PAID'", Long.class);
    if (paidOrderIds.isEmpty()) {
      return;
    }

    Collections.shuffle(paidOrderIds, new Random());

    int total = paidOrderIds.size();
    int shippingReadyCount = (int) Math.round(total * 0.25);
    int exchangeCount = (int) Math.round(total * 0.20);
    int refundCount = (int) Math.round(total * 0.15);
    // 나머지는 PAID로 그대로 둠

    updateOrderStatuses(paidOrderIds.subList(0, shippingReadyCount), "SHIPPING_READY");
    updateOrderStatuses(paidOrderIds.subList(shippingReadyCount, shippingReadyCount + exchangeCount), "EXCHANGE_REQUESTED");
    updateOrderStatuses(
            paidOrderIds.subList(shippingReadyCount + exchangeCount, shippingReadyCount + exchangeCount + refundCount),
            "REFUND_REQUESTED");

    log.info("Spread PAID orders: {} -> SHIPPING_READY, {} -> EXCHANGE_REQUESTED, {} -> REFUND_REQUESTED (총 {}건 중).",
            shippingReadyCount, exchangeCount, refundCount, total);
  }

  private void updateOrderStatuses(List<Long> onos, String status) {
    for (Long ono : onos) {
      jdbcTemplate.update("update tbl_orders set order_status = ? where ono = ?", status, ono);
    }
  }

  private YearMonth pickWeightedMonth(List<YearMonth> months, List<Integer> weights, int totalWeight, Random random) {
    int roll = random.nextInt(totalWeight);
    int cumulative = 0;
    for (int i = 0; i < months.size(); i++) {
      cumulative += weights.get(i);
      if (roll < cumulative) {
        return months.get(i);
      }
    }
    return months.get(months.size() - 1);
  }

  // 해당 월 안에서 무작위 날짜/시각 생성. 이번 달이면 미래 날짜가 나오지 않도록 오늘까지로 제한.
  private LocalDateTime randomDateTimeInMonth(YearMonth month, Random random) {
    int lastDay = month.equals(YearMonth.now())
            ? LocalDate.now().getDayOfMonth()
            : month.lengthOfMonth();

    int day = 1 + random.nextInt(lastDay);
    int hour = random.nextInt(24);
    int minute = random.nextInt(60);
    int second = random.nextInt(60);

    return month.atDay(day).atTime(hour, minute, second);
  }

  // commerce_dummy.json orders[] 1건 → tbl_orders, tbl_order_item 저장 및 OrderItem 맵 등록
  private void saveOrderFromJson(Map<String, Object> o, Map<String, OrderItem> orderItemMap) {
    String memberEmail = (String) o.get("memberEmail");
    Member member = memberRepository.findById(memberEmail).orElse(null);
    if (member == null) {
      log.warn("Skip order {}. member not found: {}", o.get("orderNumber"), memberEmail);
      return;
    }

    MemberDetail detail = memberDetailRepository.getByMemberEmail(memberEmail).orElse(null);
    String receiverName = o.containsKey("receiverName")
            ? (String) o.get("receiverName")
            : (detail != null ? detail.getName() : member.getNickname());
    String receiverPhone = o.containsKey("receiverPhone")
            ? (String) o.get("receiverPhone")
            : (detail != null ? detail.getPhone() : "010-0000-0000");

    List<Map<String, Object>> items = castList(o.get("items"));
    List<OrderItem> draftItems = new ArrayList<>();
    int totalPrice = 0;

    if (items != null) {
      for (Map<String, Object> item : items) {
        Long pno = toLongObject(item.get("productPno"));
        Product product = productRepository.findById(pno).orElse(null);
        if (product == null) {
          log.warn("Skip order item. product not found: pno={}", pno);
          continue;
        }
        int qty = toInt(item.get("qty"));
        draftItems.add(OrderItem.builder()
                .product(product)
                .pnameSnapshot(product.getPname())
                .priceSnapshot(product.getPrice())
                .qty(qty)
                .build());
        totalPrice += product.getPrice() * qty;
      }
    }

    if (draftItems.isEmpty()) {
      log.warn("Skip order {}. no valid items.", o.get("orderNumber"));
      return;
    }

    int shippingFee = o.containsKey("shippingFee")
            ? toInt(o.get("shippingFee"))
            : (totalPrice >= 50000 ? 0 : 3000);

    Orders ordersEntity = Orders.builder()
            .orderNumber((String) o.get("orderNumber"))
            .member(member)
            .totalPrice(totalPrice)
            .shippingFee(shippingFee)
            .receiverName(receiverName)
            .receiverPhone(receiverPhone)
            .zipcode((String) o.getOrDefault("zipcode", "06234"))
            .address((String) o.getOrDefault("address", "서울특별시 강남구 테헤란로 123"))
            .addressDetail((String) o.get("addressDetail"))
            .orderStatus((String) o.get("orderStatus"))
            .build();

    draftItems.forEach(ordersEntity::addOrderItem);
    Orders saved = orderRepository.save(ordersEntity);

    for (OrderItem orderItem : saved.getOrderItems()) {
      orderItemMap.put(orderItemKey(saved.getOrderNumber(), orderItem.getProduct().getPno()), orderItem);
    }
  }

  // commerce_dummy.json reviews[] 1건 → tbl_review, adminReply 있으면 관리자 답글(tbl_review)
  private void saveReviewFromJson(Map<String, Object> r, Map<String, OrderItem> orderItemMap, Member admin) {
    String key = orderItemKey((String) r.get("orderNumber"), toLongObject(r.get("productPno")));
    OrderItem orderItem = orderItemMap.get(key);
    if (orderItem == null) {
      log.warn("Skip review. order item not found: {}", key);
      return;
    }

    Review review = Review.builder()
            .product(orderItem.getProduct())
            .member(orderItem.getOrders().getMember())
            .orderItem(orderItem)
            .rating(toIntegerObject(r.get("rating")))
            .content((String) r.get("content"))
            .build();
    Review saved = reviewRepository.save(review);

    String adminReply = (String) r.get("adminReply");
    if (admin != null && adminReply != null && !adminReply.isBlank()) {
      reviewRepository.save(Review.builder()
              .product(orderItem.getProduct())
              .member(admin)
              .orderItem(orderItem)
              .review(saved)
              .rating(null)
              .content(adminReply)
              .build());
    }
  }

  // commerce_dummy.json qnas[] 1건 → tbl_qna, adminAnswer 있으면 ADMIN 답변(tbl_qna)
  private void saveQnaFromJson(Map<String, Object> q, Member admin) {
    String memberEmail = (String) q.get("memberEmail");
    Long pno = toLongObject(q.get("productPno"));
    Member member = memberRepository.findById(memberEmail).orElse(null);
    Product product = productRepository.findById(pno).orElse(null);
    if (member == null || product == null) {
      log.warn("Skip qna. member={}, pno={}", memberEmail, pno);
      return;
    }

    Qna question = qnaRepository.save(Qna.builder()
            .product(product)
            .member(member)
            .content((String) q.get("content"))
            .build());

    String adminAnswer = (String) q.get("adminAnswer");
    if (admin != null && adminAnswer != null && !adminAnswer.isBlank()) {
      qnaRepository.save(Qna.builder()
              .product(product)
              .member(admin)
              .qna(question)
              .content(adminAnswer)
              .build());
    }
  }

  // DB tbl_review 집계 → tbl_product 평균평점·리뷰수 갱신
  private void refreshAllProductRatingStats(List<Product> products) {
    for (Product product : products) {
      Long pno = product.getPno();
      Double avg = reviewRepository.getAverageRating(pno);
      long count = reviewRepository.countByProduct(pno);
      product.changeRatingStats(avg == null ? 0 : avg, (int) count);
      productRepository.save(product);
    }
  }

  // 리뷰 FK 연결용 OrderItem 조회 키 (orderNumber:productPno)
  private String orderItemKey(String orderNumber, Long productPno) {
    return orderNumber + ":" + productPno;
  }

  // member.json 날짜 필드 파싱 헬퍼 (suspendUntil, lastLoginAt)
  private LocalDateTime parseDateTime(String value) {
    return (value == null || value.isBlank()) ? null : LocalDateTime.parse(value);
  }

  // makeup.json cmno → DB tbl_company 업체 조회 (FK 연결용)
  private Company findCompany(Object cmno) {
    Long companyId = toLongObject(cmno);
    return companyRepository.findById(companyId)
            .orElseGet(() -> {
              log.warn("Skip makeup package. Company not found for cmno={}", companyId);
              return null;
            });
  }

  // classpath JSON 배열 읽기 (data/product.json, data/member.json, data/makeup.json)
  private List<Map<String, Object>> readJsonArray(String path) throws Exception {
    ClassPathResource resource = new ClassPathResource(path);
    try (InputStream is = resource.getInputStream()) {
      return objectMapper.readValue(is, new TypeReference<>() {});
    }
  }

  // classpath JSON 객체 읽기 (data/commerce_dummy.json)
  private Map<String, Object> readJsonObject(String path) throws Exception {
    ClassPathResource resource = new ClassPathResource(path);
    try (InputStream is = resource.getInputStream()) {
      return objectMapper.readValue(is, new TypeReference<>() {});
    }
  }

  // JSON 배열 필드 List 캐스팅 헬퍼
  @SuppressWarnings("unchecked")
  private <T> List<T> castList(Object value) {
    return value instanceof List<?> ? (List<T>) value : null;
  }

  // JSON enum 문자열 변환 헬퍼
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

  // makeup.json packageType 레거시 값 정규화
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
    if ("THREE".equals(name) || "NAIL_MAKEUP".equals(name)) {
      return MakeupPackageType.FULL;
    }
    return enumValue(MakeupPackageType.class, name, MakeupPackageType.MAKEUP);
  }

  // JSON 숫자 → int 변환
  private int toInt(Object value) {
    Integer result = toIntegerObject(value);
    return result == null ? 0 : result;
  }

  // JSON 숫자 → Integer 변환
  private Integer toIntegerObject(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Number number) {
      return number.intValue();
    }
    return Integer.parseInt(value.toString());
  }

  // JSON 숫자 → Long 변환 (productPno, makeup.json cmno 등)
  private Long toLongObject(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Number number) {
      return number.longValue();
    }
    return Long.parseLong(value.toString());
  }

  // JSON 숫자 → BigDecimal 변환 (makeup.json discountRate 등)
  private BigDecimal toBigDecimal(Object value) {
    if (value == null) {
      return BigDecimal.ZERO;
    }
    return new BigDecimal(value.toString());
  }

  // JSON 숫자 → BigDecimal 변환 (null 허용 - 메이크업 가격 등)
  private BigDecimal toBigDecimalNullable(Object value) {
    if (value == null) {
      return null;
    }
    return new BigDecimal(value.toString());
  }

  // JSON 숫자 → Double 변환 (위도/경도)
  private Double toDouble(Object value) {
    if (value == null) return null;
    if (value instanceof Number number) return number.doubleValue();
    return Double.parseDouble(value.toString());
  }
}