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
import com.wedding.coupleprofile.domain.CoupleProfile;
import com.wedding.coupleprofile.repository.CoupleProfileRepository;
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
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
  private final CoupleProfileRepository coupleProfileRepository;

  // 애플리케이션 기동 시 더미 시드 진입점 (모든 시드: 해당 테이블 count == 0 일 때만)
  // 1) data/product.json         → tbl_product, tbl_product_option
  // 2) data/member.json          → tbl_member, tbl_member_detail
  // 3) data/board.json           → tbl_board (작성자는 ACTIVE 회원 순환 배정)
  // 4) data/comment.json         → tbl_comment (작성자는 ACTIVE 회원 순환 배정, board 이후)
  // 5) data/commerce_dummy.json  → tbl_orders, tbl_order_item, tbl_review, tbl_qna
  // 6) data/couple_profile.json  → tbl_couple_profile (회원 이후)
  // 7) data/makeup.json          → tbl_makeup_package (업체 DB 있을 때만)
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

    if (coupleProfileRepository.count() == 0) {
      log.info("===== Couple profile dummy seed start =====");
      insertCoupleProfiles();
      log.info("===== Couple profile dummy seed complete =====");
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
      }
    } else {
      relaxLegacyDetailColumns();
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

  // data/couple_profile.json → tbl_couple_profile. 실제 회원 이메일과 연결된 선배부부 프로필
  private void insertCoupleProfiles() throws Exception {
    List<Map<String, Object>> list = readJsonArray("data/couple_profile.json");
    List<CoupleProfile> profiles = new ArrayList<>();

    for (Map<String, Object> m : list) {
      String memberEmail = (String) m.get("memberEmail");

      if (!memberRepository.existsById(memberEmail)) {
        log.warn("Skip couple profile. Member not found: {}", memberEmail);
        continue;
      }

      profiles.add(CoupleProfile.builder()
              .memberEmail(memberEmail)
              .budgetMin(toInt(m.get("budgetMin")))
              .budgetMax(toInt(m.get("budgetMax")))
              .region((String) m.get("region"))
              .weddingStyle((String) m.get("weddingStyle"))
              .weddingDate(LocalDate.parse((String) m.get("weddingDate")))
              .bio((String) m.get("bio"))
              .build());
    }

    coupleProfileRepository.saveAll(profiles);
    log.info("Inserted {} couple profiles.", profiles.size());
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
