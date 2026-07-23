package com.wedding.common.init;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedding.admin.dashboard.domain.AiBriefing;
import com.wedding.admin.dashboard.domain.FlaggedPost;
import com.wedding.admin.dashboard.domain.SiteHealthIssue;
import com.wedding.admin.dashboard.repository.AiBriefingRepository;
import com.wedding.admin.dashboard.repository.FlaggedPostRepository;
import com.wedding.admin.dashboard.repository.SiteHealthIssueRepository;
import com.wedding.global.util.CustomFileUtil;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import com.wedding.board.domain.Board;
import com.wedding.board.domain.Comment;
import com.wedding.board.repository.BoardRepository;
import com.wedding.board.repository.CommentRepository;
import com.wedding.checkout.domain.OrderItem;
import com.wedding.checkout.domain.Orders;
import com.wedding.checkout.repository.OrderRepository;
import com.wedding.company.domain.Company;
import com.wedding.company.domain.CompanyCategory;
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
  // OpenClaw 일간 점검 더미데이터 (사이트 이상 징후 / 확인 필요한 게시글) - 관리자 대시보드 옆
  // 플로팅 패널에서 바로 보이도록 시딩
  private final SiteHealthIssueRepository siteHealthIssueRepository;
  private final FlaggedPostRepository flaggedPostRepository;
  private final AiBriefingRepository aiBriefingRepository;
  private final CustomFileUtil customFileUtil;

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
      forceRecentDemoBoardDates();
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

    if (siteHealthIssueRepository.count() == 0 && flaggedPostRepository.count() == 0) {
      log.info("===== OpenClaw ops dummy seed start =====");
      insertOpsDummyData();
      log.info("===== OpenClaw ops dummy seed complete =====");
    }

    if (aiBriefingRepository.count() == 0) {
      log.info("===== AI briefing dummy seed start =====");
      insertAiBriefingDummy();
      log.info("===== AI briefing dummy seed complete =====");
    }
  }

  // /admin/ai-briefing 페이지가 비어있지 않도록, 지금으로부터 정확히 1주일 전 날짜로
  // 실제 DB 수치를 반영한 그럴듯한 한글 리포트 PDF를 만들어 시딩한다.
  // (실제로는 OpenClaw가 매주 월요일 새벽 POST /api/openclaw/ai-briefing 으로 채움)
  // 한글 표시를 위해 이 PC에 있는 맑은 고딕(Windows 기본 폰트)을 그대로 임베드해서 사용한다.
  private void insertAiBriefingDummy() {

    LocalDate weekOfDate = LocalDate.now().minusDays(7);
    String weekOfLabel = weekOfDate.toString();

    long memberCount = memberRepository.count();
    long totalRevenue = orderRepository.sumTotalRevenue();
    long productCount = productRepository.countByDelFlagFalse();
    long lowStockCount = productRepository.countByDelFlagFalseAndStockQtyLessThanEqual(5);
    long boardCount = boardRepository.count();
    long companyCount = companyRepository.count();
    long siteIssueCount = siteHealthIssueRepository.countByResolvedFalse();
    long flaggedPostCount = flaggedPostRepository.countByResolvedFalse();

    // 바 그래프용 - 주문 상태별 건수
    long[] orderStatusValues = new long[]{
        orderRepository.countByOrderStatus("PAID"),
        orderRepository.countByOrderStatus("SHIPPING"),
        orderRepository.countByOrderStatus("DELIVERED"),
    };
    String[] orderStatusLabels = new String[]{"결제완료", "배송중", "배송완료"};

    // 원형 그래프용 - 업체 카테고리별 등록 수 (전체 업체를 한 번만 읽어서 자바에서 집계)
    List<Company> allCompanies = companyRepository.findAll();
    long[] categoryValues = new long[]{
        allCompanies.stream().filter(c -> c.getCategory() == CompanyCategory.HALL).count(),
        allCompanies.stream().filter(c -> c.getCategory() == CompanyCategory.DRESS).count(),
        allCompanies.stream().filter(c -> c.getCategory() == CompanyCategory.STUDIO).count(),
        allCompanies.stream().filter(c -> c.getCategory() == CompanyCategory.MAKEUP).count(),
    };
    String[] categoryLabels = new String[]{"웨딩홀", "드레스", "스튜디오", "메이크업"};

    String revenueText = String.format("%,d", totalRevenue) + "원";

    List<String[]> sections = new ArrayList<>();
    sections.add(new String[]{"1. 이번 주 한눈에 요약",
        "지난 한 주간 누적 매출은 " + revenueText + "을 기록했으며, 전체 회원 " + memberCount
            + "명이 서비스를 이용 중입니다. 전반적으로 안정적인 운영 흐름을 유지했습니다."});
    sections.add(new String[]{"2. 회원 동향",
        "현재 전체 회원 수는 " + memberCount + "명입니다. 신규 가입 추이는 꾸준한 편이며,",
        "이메일 미인증 계정에 대한 후속 안내를 검토해볼 시점입니다."});
    sections.add(new String[]{"3. 업체 현황",
        "등록된 업체는 총 " + companyCount + "곳입니다. 웨딩홀 카테고리의 매출 편차가 큰 편이라,",
        "하위권 업체에 대한 노출 지원 방안을 검토하면 좋을 것 같습니다."});
    sections.add(new String[]{"4. 상품 라인업 인사이트",
        "현재 판매 중인 답례품은 " + productCount + "종입니다. 특정 카테고리(식기·타월 등)에",
        "상품이 몰려 있는 편이라, 프리미엄/실속형 라인업 확충을 검토해보면 좋겠습니다."});
    sections.add(new String[]{"5. 매출 추이",
        "누적 매출은 " + revenueText + "이며, 최근 추이는 대체로 완만한 상승세를 보이고 있습니다."});
    sections.add(new String[]{"6. 고객 반응 (리뷰 / QnA)",
        "커뮤니티 게시글은 총 " + boardCount + "건이 누적되어 있습니다. 신규 리뷰 유입은 꾸준하나,",
        "미답변 문의에 대한 빠른 대응이 권장됩니다."});
    sections.add(new String[]{"7. 요즘 추세 참고",
        "이번 주는 특별히 주목할 만한 웨딩업계 트렌드 이슈는 확인되지 않았습니다."});
    sections.add(new String[]{"8. 이번 주 일간 체크 회고",
        "사이트 이상 징후 " + siteIssueCount + "건, 확인 필요한 게시글 " + flaggedPostCount + "건,",
        "재고 부족 상품 " + lowStockCount + "건이 감지되어 대시보드에 반영되었습니다."});
    sections.add(new String[]{"9. 종합 총평 + 다음 주 체크리스트",
        "전반적으로 서비스는 안정적으로 운영되고 있습니다. 다음 주에는 아래 항목을 우선 확인해주세요.",
        "- 미답변 Q&A 처리", "- 저평점 리뷰 확인 및 응대", "- 답례품 프리미엄 라인업 검토"});

    String summaryText = "지난 한 주 누적 매출 " + revenueText + ", 회원 " + memberCount
        + "명. 답례품 카테고리 구성과 미답변 문의 대응을 우선 점검하면 좋겠습니다. (더미 데이터)";

    byte[] pdfBytes = renderBriefingPdf(weekOfLabel, sections, memberCount, totalRevenue, productCount,
        companyCount, orderStatusLabels, orderStatusValues, categoryLabels, categoryValues);
    String savedFileName = customFileUtil.saveBytes(pdfBytes, "weekly_briefing_" + weekOfLabel + ".pdf");

    AiBriefing briefing = AiBriefing.builder()
        .weekOf(weekOfLabel)
        .summaryText(summaryText)
        .pdfFileName(savedFileName)
        .lowStockCount((int) lowStockCount)
        .flaggedPostCount((int) flaggedPostCount)
        .siteIssueCount((int) siteIssueCount)
        .build();

    aiBriefingRepository.save(briefing);
  }

  // 브랜드 팔레트 (프론트 대시보드 색상과 맞춤 - AdminDashboardComponent.jsx의 차트 색상 재사용)
  private static final Color COLOR_PINK = new Color(0xC0, 0x60, 0x80);
  private static final Color COLOR_LAVENDER = new Color(0xC5, 0xB3, 0xD3);
  private static final Color COLOR_GREEN = new Color(0x7F, 0xB0, 0x69);
  private static final Color COLOR_AMBER = new Color(0xD9, 0xA6, 0x5B);
  private static final Color COLOR_CREAM = new Color(0xF7, 0xF3, 0xEE);
  private static final Color COLOR_INK = new Color(0x3A, 0x36, 0x2F);
  private static final Color[] CHART_PALETTE = {COLOR_PINK, COLOR_LAVENDER, COLOR_GREEN, COLOR_AMBER};

  // 맑은 고딕(이 PC의 Windows 기본 폰트)을 임베드해서 한글이 제대로 보이는 리포트 PDF를 생성한다.
  // KPI 박스 + 막대그래프(주문 상태) + 원형그래프(업체 카테고리 분포)를 상단에 그리고, 아래에 텍스트 섹션을 이어붙인다.
  private byte[] renderBriefingPdf(String weekOfLabel, List<String[]> sections,
      long memberCount, long totalRevenue, long productCount, long companyCount,
      String[] barLabels, long[] barValues, String[] pieLabels, long[] pieValues) {

    float margin = 50f;
    float pageWidth = PDRectangle.A4.getWidth();
    float pageHeight = PDRectangle.A4.getHeight();
    float maxTextWidth = pageWidth - margin * 2;

    try (PDDocument document = new PDDocument()) {

      PDFont regularFont = PDType0Font.load(document, new File("C:/Windows/Fonts/malgun.ttf"));
      PDFont boldFont = PDType0Font.load(document, new File("C:/Windows/Fonts/malgunbd.ttf"));

      PDPage page = new PDPage(PDRectangle.A4);
      document.addPage(page);
      PDPageContentStream cs = new PDPageContentStream(document, page);

      float y = pageHeight - margin;

      cs.setNonStrokingColor(COLOR_PINK);
      y = drawLine(cs, boldFont, 22, margin, y, "AI 매니저 주간 브리핑");
      cs.setNonStrokingColor(COLOR_INK);
      y -= 4;
      y = drawLine(cs, regularFont, 10, margin, y, "주차: " + weekOfLabel + " (지난주)  ·  생성일: " + LocalDate.now());
      y -= 4;
      fillRect(cs, margin, y, pageWidth - margin * 2, 2, COLOR_PINK);
      y -= 22;

      // ===== 핵심 지표 KPI 박스 4개 =====
      String[] kpiLabels = {"누적 매출", "전체 회원", "판매 상품", "등록 업체"};
      String[] kpiValues = {
          String.format("%,d", totalRevenue) + "원", memberCount + "명", productCount + "종", companyCount + "곳"
      };
      y = drawKpiRow(cs, regularFont, boldFont, margin, y, pageWidth - margin * 2, kpiLabels, kpiValues);
      y -= 26;

      // ===== 막대그래프 + 원형그래프를 나란히 =====
      // 막대는 baseline에서 위로 최대 chartHeight만큼 자라므로, baseline은
      // "제목 아래로 titleGap + chartHeight"만큼 내려간 지점이어야 막대 끝이 제목과 안 겹침
      float chartsTop = y;
      float chartAreaWidth = (pageWidth - margin * 2 - 30) / 2f;
      float titleGap = 22f;
      float chartHeight = 110f;
      float baselineY = chartsTop - titleGap - chartHeight;

      cs.setNonStrokingColor(COLOR_INK);
      drawLine(cs, boldFont, 12, margin, chartsTop, "주문 상태 현황");
      drawBarChart(cs, regularFont, boldFont, margin, baselineY, chartAreaWidth, chartHeight, barLabels, barValues, CHART_PALETTE);

      float pieX = margin + chartAreaWidth + 30;
      float pieRadius = 50f;
      drawLine(cs, boldFont, 12, pieX, chartsTop, "업체 카테고리 분포");
      float pieCenterX = pieX + pieRadius + 5;
      float pieCenterY = baselineY + pieRadius;
      drawPieChart(cs, pieCenterX, pieCenterY, pieRadius, pieValues, CHART_PALETTE);
      drawLegend(cs, regularFont, pieCenterX + pieRadius + 20, chartsTop - titleGap, pieLabels, pieValues, CHART_PALETTE);

      y = baselineY - 40;

      fillRect(cs, margin, y, pageWidth - margin * 2, 1, COLOR_CREAM);
      y -= 20;

      for (String[] section : sections) {
        if (y < margin + 60) {
          cs.close();
          page = new PDPage(PDRectangle.A4);
          document.addPage(page);
          cs = new PDPageContentStream(document, page);
          y = pageHeight - margin;
        }

        cs.setNonStrokingColor(COLOR_PINK);
        y = drawLine(cs, boldFont, 13, margin, y, section[0]);
        cs.setNonStrokingColor(COLOR_INK);
        y -= 4;

        for (int i = 1; i < section.length; i++) {
          for (String wrapped : wrapText(regularFont, 11f, maxTextWidth, section[i])) {
            y = drawLine(cs, regularFont, 11, margin, y, wrapped);
          }
        }
        y -= 14;
      }

      cs.close();

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      document.save(out);
      return out.toByteArray();

    } catch (Exception e) {
      throw new RuntimeException("주간 브리핑 더미 PDF 생성 실패: " + e.getMessage(), e);
    }
  }

  private float drawLine(PDPageContentStream cs, PDFont font, float fontSize, float x, float y, String text) {
    try {
      cs.beginText();
      cs.setFont(font, fontSize);
      cs.newLineAtOffset(x, y);
      cs.showText(text);
      cs.endText();
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
    return y - (fontSize + 6);
  }

  private void fillRect(PDPageContentStream cs, float x, float y, float w, float h, Color color) throws IOException {
    cs.setNonStrokingColor(color);
    cs.addRect(x, y, w, h);
    cs.fill();
  }

  private void drawCenteredText(PDPageContentStream cs, PDFont font, float size, float xStart, float xEnd,
      float y, String text) throws IOException {
    float width = stringWidth(font, size, text);
    float x = xStart + ((xEnd - xStart) - width) / 2f;
    cs.beginText();
    cs.setFont(font, size);
    cs.newLineAtOffset(x, y);
    cs.showText(text);
    cs.endText();
  }

  // 상단 KPI 박스 4개를 색상 있는 카드 형태로 그린다
  private float drawKpiRow(PDPageContentStream cs, PDFont font, PDFont boldFont, float x, float y, float totalWidth,
      String[] labels, String[] values) throws IOException {

    int n = labels.length;
    float gap = 10f;
    float boxWidth = (totalWidth - gap * (n - 1)) / n;
    float boxHeight = 46f;
    float boxTop = y;

    for (int i = 0; i < n; i++) {
      float bx = x + i * (boxWidth + gap);
      float by = boxTop - boxHeight;

      fillRect(cs, bx, by, boxWidth, boxHeight, COLOR_CREAM);

      cs.setNonStrokingColor(CHART_PALETTE[i % CHART_PALETTE.length]);
      drawCenteredText(cs, boldFont, 13, bx, bx + boxWidth, by + boxHeight - 20, values[i]);
      cs.setNonStrokingColor(COLOR_INK);
      drawCenteredText(cs, font, 9, bx, bx + boxWidth, by + 10, labels[i]);
    }

    return boxTop - boxHeight;
  }

  // 막대그래프 - baseline(y)에서 위로 자라는 막대들, 값/라벨을 각각 위/아래에 표기
  private void drawBarChart(PDPageContentStream cs, PDFont font, PDFont boldFont, float x, float y,
      float width, float chartHeight, String[] labels, long[] values, Color[] colors) throws IOException {

    long max = 1;
    for (long v : values) {
      max = Math.max(max, v);
    }

    int n = values.length;
    float gap = 16f;
    float barWidth = (width - gap * (n - 1)) / n;

    for (int i = 0; i < n; i++) {
      float barHeight = values[i] <= 0 ? 2f : Math.max(4f, (values[i] / (float) max) * chartHeight);
      float bx = x + i * (barWidth + gap);

      fillRect(cs, bx, y, barWidth, barHeight, colors[i % colors.length]);

      cs.setNonStrokingColor(COLOR_INK);
      drawCenteredText(cs, boldFont, 10, bx, bx + barWidth, y + barHeight + 5, String.valueOf(values[i]));
      drawCenteredText(cs, font, 9, bx, bx + barWidth, y - 13, labels[i]);
    }
  }

  // 원형그래프 - 12시 방향에서 시계방향으로 값 비율만큼 색칠된 부채꼴을 이어붙임
  private void drawPieChart(PDPageContentStream cs, float cx, float cy, float radius,
      long[] values, Color[] colors) throws IOException {

    long total = 0;
    for (long v : values) {
      total += v;
    }
    if (total <= 0) {
      total = 1;
    }

    float angle = 90f;
    for (int i = 0; i < values.length; i++) {
      float sweep = (values[i] / (float) total) * 360f;
      if (sweep > 0) {
        drawPieSlice(cs, cx, cy, radius, angle, angle - sweep, colors[i % colors.length]);
      }
      angle -= sweep;
    }
  }

  private void drawPieSlice(PDPageContentStream cs, float cx, float cy, float radius,
      float startDeg, float endDeg, Color color) throws IOException {

    cs.setNonStrokingColor(color);
    cs.moveTo(cx, cy);

    int steps = Math.max(2, (int) (Math.abs(startDeg - endDeg) / 3f));
    float stepAngle = (endDeg - startDeg) / steps;

    for (int i = 0; i <= steps; i++) {
      double angleRad = Math.toRadians(startDeg + stepAngle * i);
      float px = (float) (cx + radius * Math.cos(angleRad));
      float py = (float) (cy + radius * Math.sin(angleRad));
      cs.lineTo(px, py);
    }
    cs.closePath();
    cs.fill();
  }

  // 원형그래프 옆에 색상 네모 + 라벨 + 값으로 된 범례
  private void drawLegend(PDPageContentStream cs, PDFont font, float x, float y,
      String[] labels, long[] values, Color[] colors) throws IOException {

    float rowHeight = 16f;
    for (int i = 0; i < labels.length; i++) {
      float ly = y - i * rowHeight - 10;
      fillRect(cs, x, ly, 9, 9, colors[i % colors.length]);

      cs.setNonStrokingColor(COLOR_INK);
      cs.beginText();
      cs.setFont(font, 9);
      cs.newLineAtOffset(x + 14, ly + 1);
      cs.showText(labels[i] + " " + values[i] + "곳");
      cs.endText();
    }
  }

  // 지정한 너비를 넘으면 공백 기준으로 다음 줄로 넘기는 간단한 워드랩
  private List<String> wrapText(PDFont font, float fontSize, float maxWidth, String text) {
    List<String> lines = new ArrayList<>();
    String[] words = text.split(" ");
    StringBuilder current = new StringBuilder();

    for (String word : words) {
      String candidate = current.length() == 0 ? word : current + " " + word;
      float width = stringWidth(font, fontSize, candidate);

      if (width > maxWidth && current.length() > 0) {
        lines.add(current.toString());
        current = new StringBuilder(word);
      } else {
        current = new StringBuilder(candidate);
      }
    }

    if (current.length() > 0) {
      lines.add(current.toString());
    }

    return lines;
  }

  private float stringWidth(PDFont font, float fontSize, String text) {
    try {
      return font.getStringWidth(text) / 1000 * fontSize;
    } catch (Exception e) {
      return 0;
    }
  }

  // 관리자 대시보드 옆 플로팅 패널이 비어있지 않도록 만드는 더미데이터.
  // 실제로는 OpenClaw가 매일 새벽 POST /api/openclaw/site-health-issues, /flagged-posts 로 채움.
  private void insertOpsDummyData() {

    siteHealthIssueRepository.save(SiteHealthIssue.builder()
        .pageUrl("http://localhost:3000/product/list")
        .issueType("IMAGE_BROKEN")
        .detail("답례품 카테고리 3번째 상품 썸네일 이미지가 로드되지 않습니다.")
        .build());

    siteHealthIssueRepository.save(SiteHealthIssue.builder()
        .pageUrl("http://localhost:3000/product/list")
        .issueType("IMAGE_BROKEN")
        .detail("답례품 카테고리 7번째 상품에 실제 사진이 없어 기본 이미지가 대신 표시되고 있습니다.")
        .build());

    // 실제로 board.json에 심어둔 데모용 문제 게시글(스팸/저격/욕설)과 매칭시켜서
    // 플로팅 패널의 고정 더미도 실제 상황과 어긋나지 않게 함
    List<Board> sampleBoards = boardRepository.findAll();
    Long boardIdForSpam = findBoardIdByContentMarker(sampleBoards, "addme1234");
    Long boardIdForHostile = findBoardIdByContentMarker(sampleBoards, "눈치 좀 챙기세요");
    Long boardIdForProfanity = findBoardIdByContentMarker(sampleBoards, "지랄맞게");

    flaggedPostRepository.save(FlaggedPost.builder()
        .boardId(boardIdForSpam)
        .reason("동일한 문구가 짧은 시간 내 반복 게시되어 광고성 도배 글로 의심됩니다.")
        .build());

    flaggedPostRepository.save(FlaggedPost.builder()
        .boardId(boardIdForHostile)
        .reason("특정 회원을 반복적으로 저격하는 듯한 표현이 있어 확인이 필요합니다.")
        .build());

    flaggedPostRepository.save(FlaggedPost.builder()
        .boardId(boardIdForProfanity)
        .reason("다른 회원을 향한 명확한 욕설/비하 표현이 포함되어 있어 확인이 필요합니다.")
        .build());
  }

  private Long findBoardIdByContentMarker(List<Board> boards, String marker) {
    return boards.stream()
        .filter(b -> b.getContent() != null && b.getContent().contains(marker))
        .map(Board::getBoardId)
        .findFirst()
        .orElse(boards.isEmpty() ? 1L : boards.get(0).getBoardId());
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

    // 업체 현황 "월별 매출 추이"용 - paidAt을 최근 6개월에 걸쳐 가중치를 두고(최근 달일수록 많이) 분산
    int monthsBack = 5;
    YearMonth currentMonth = YearMonth.now();
    List<YearMonth> months = new ArrayList<>();
    for (int m = monthsBack; m >= 0; m--) {
      months.add(currentMonth.minusMonths(m));
    }
    List<Integer> weights = new ArrayList<>();
    for (int w = 1; w <= months.size(); w++) {
      weights.add(w);
    }
    int totalWeight = weights.stream().mapToInt(Integer::intValue).sum();
    Random random = new Random();

    for (Company company : companyMap.values()) {
      int purchaseCount = companyPurchaseCountMap.getOrDefault(company.getCmno(), 0);

      for (int n = 0; n < purchaseCount; n++) {
        YearMonth paidMonth = pickWeightedMonth(months, weights, totalWeight, random);
        LocalDateTime paidAt = randomDateTimeInMonth(paidMonth, random);

        Reservation dummy = Reservation.builder()
                .cmno(company.getCmno())
                .memberEmail("dummy" + (n % 20) + "@wedding.demo")
                .weddingDate(baseDate.plusDays((i * 3) % 180))
                .status("확정")
                .optionName("-")
                .amount(company.getPriceAvg() != null ? company.getPriceAvg().intValue() : 100000)
                .payStatus("PAID")
                .paidAt(paidAt)
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

    // 다른 더미(주문/리뷰/QnA 등)와 동일하게, member.json에 박혀있는 고정 날짜 대신
    // 시딩 실행 시점(now()) 기준 최근 6개월에 걸쳐 상대적으로 분산시킴
    spreadMemberRegDates();

    log.info("Inserted {} members (password: 1111).", memberRepository.count());
  }

  // Member는 email(String) PK라 spreadRecentRegDates(Long PK 전용)를 못 그대로 재사용해서 별도로 작성.
  // 주문/예약과 같은 6개월 창 + 최근일수록 가중치 패턴을 그대로 따름.
  private void spreadMemberRegDates() {
    List<String> emails = jdbcTemplate.queryForList("select email from member", String.class);
    if (emails.isEmpty()) {
      return;
    }

    int monthsBack = 5; // 이번 달 포함 총 6개월
    YearMonth currentMonth = YearMonth.now();
    List<YearMonth> months = new ArrayList<>();
    for (int i = monthsBack; i >= 0; i--) {
      months.add(currentMonth.minusMonths(i));
    }

    List<Integer> weights = new ArrayList<>();
    for (int i = 1; i <= months.size(); i++) {
      weights.add(i);
    }
    int totalWeight = weights.stream().mapToInt(Integer::intValue).sum();

    Random random = new Random();

    for (String email : emails) {
      YearMonth month = pickWeightedMonth(months, weights, totalWeight, random);
      LocalDateTime regDate = randomDateTimeInMonth(month, random);

      jdbcTemplate.update("update member set regdate = ? where email = ?", regDate, email);
    }

    log.info("Spread {} member regdate values across {} months ({} ~ {}).",
            emails.size(), months.size(), months.get(0), months.get(months.size() - 1));
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

  // insertBoards()의 daysAgo = i % 45 만으로는 board.json 끝에 추가로 심어둔
  // 데모용 욕설 게시글까지 "오늘"로 보장할 수 없어서, 이 글만 별도로 날짜를 고정함.
  private void forceRecentDemoBoardDates() {
    jdbcTemplate.update(
        "update tbl_board set reg_date = ? where content like ?",
        LocalDateTime.now(), "%지랄맞게%");
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
      refreshProductSalesCount();
    }

    List<Map<String, Object>> reviews = castList(root.get("reviews"));
    if (reviews != null) {
      for (Map<String, Object> r : reviews) {
        saveReviewFromJson(r, orderItemMap, admin);
      }
      spreadReviewRegDates();
      forceRecentDemoReviewDate();
    }

    List<Map<String, Object>> qnas = castList(root.get("qnas"));
    if (qnas != null) {
      for (Map<String, Object> q : qnas) {
        saveQnaFromJson(q, admin);
      }
      spreadQnaRegDates();
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

  // 리뷰도 orders와 같은 이유로 전부 "지금" 시각에 몰려있어서, OpenClaw 일간 체크가
  // "어제 올라온 리뷰만" 걸러보려 해도 항상 0건만 나옴. 최근 30일에 걸쳐 분산시킴
  // (최근일수록 조금 더 많도록 가중치를 둬서 실제 서비스처럼 보이게 함).
  private void spreadReviewRegDates() {
    spreadRecentRegDates("tbl_review", "rno");
  }

  // spreadReviewRegDates()의 가중 랜덤 분산은 "오늘"에 뭐가 걸릴지 매번 달라서,
  // OpenClaw 일간 체크(최근 24시간 리뷰만 검사)가 실제로 걸러낼 게 있는 데모용 리뷰가
  // 항상 최근 24시간 이내에 있도록 별도로 날짜를 고정함.
  private void forceRecentDemoReviewDate() {
    jdbcTemplate.update(
        "update tbl_review set regdate = ? where content like ?",
        LocalDateTime.now(), "%addme1234%");
  }

  // QnA도 리뷰와 동일한 문제라 같은 방식으로 분산
  private void spreadQnaRegDates() {
    spreadRecentRegDates("tbl_qna", "qno");
  }

  // 최근 N일에 걸쳐 regdate를 무작위로(최근일수록 가중치 높게) 재분배하는 공용 헬퍼
  private void spreadRecentRegDates(String table, String idColumn) {
    List<Long> ids = jdbcTemplate.queryForList("select " + idColumn + " from " + table, Long.class);
    if (ids.isEmpty()) {
      return;
    }

    int daysBack = 29; // 오늘 포함 총 30일
    List<Integer> weights = new ArrayList<>();
    for (int i = 1; i <= daysBack + 1; i++) {
      weights.add(i); // 오래된 날일수록 가중치 낮게, 오늘에 가까울수록 높게
    }
    int totalWeight = weights.stream().mapToInt(Integer::intValue).sum();

    Random random = new Random();

    for (Long id : ids) {
      int daysAgo = pickWeightedDaysAgo(daysBack, weights, totalWeight, random);
      LocalDateTime regDate = LocalDateTime.now()
              .minusDays(daysAgo)
              .withHour(random.nextInt(24))
              .withMinute(random.nextInt(60))
              .withSecond(random.nextInt(60));

      jdbcTemplate.update("update " + table + " set regdate = ? where " + idColumn + " = ?", regDate, id);
    }

    log.info("Spread {} regdate values in {} across the last {} days.", ids.size(), table, daysBack + 1);
  }

  // weights[0]이 daysBack(가장 오래전)에 대응하고 마지막이 0일(오늘)에 대응하도록 역순으로 뽑음
  private int pickWeightedDaysAgo(int daysBack, List<Integer> weights, int totalWeight, Random random) {
    int r = random.nextInt(totalWeight);
    int cumulative = 0;
    for (int i = 0; i < weights.size(); i++) {
      cumulative += weights.get(i);
      if (r < cumulative) {
        return daysBack - i;
      }
    }
    return 0;
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

  // 실제 결제 로직은 주문 완료 시 Product.salesCount를 올리지만, 더미 주문은 JDBC로 직접 꽂아 넣어서
  // 이 로직을 안 타기 때문에 전 상품이 salesCount=0으로 남아있었음. 주문 다 넣은 뒤 한 번에 집계해서 채움.
  private void refreshProductSalesCount() {
    jdbcTemplate.update("update tbl_product set sales_count = 0");
    List<Map<String, Object>> totals = jdbcTemplate.queryForList(
            "select product_pno, sum(qty) as total_qty from tbl_order_item group by product_pno");
    for (Map<String, Object> row : totals) {
      Long pno = ((Number) row.get("product_pno")).longValue();
      int totalQty = ((Number) row.get("total_qty")).intValue();
      jdbcTemplate.update("update tbl_product set sales_count = ? where pno = ?", totalQty, pno);
    }
    log.info("Refreshed sales_count for {} products from tbl_order_item.", totals.size());
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