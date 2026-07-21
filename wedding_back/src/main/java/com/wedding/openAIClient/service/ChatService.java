package com.wedding.openAIClient.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedding.company.domain.Company;
import com.wedding.company.domain.CompanyCategory;
import com.wedding.company.domain.DressItem;
import com.wedding.company.domain.DressItemType;
import com.wedding.company.domain.HallItem;
import com.wedding.company.domain.MakeupPackageType;
import com.wedding.company.domain.MealType;
import com.wedding.company.dto.CompanyDTO;
import com.wedding.company.dto.DressDetailDTO;
import com.wedding.company.dto.DressItemDTO;
import com.wedding.company.dto.HallDetailDTO;
import com.wedding.company.dto.HallItemDTO;
import com.wedding.company.dto.MakeupDetailDTO;
import com.wedding.company.dto.MakeupPackageDTO;
import com.wedding.company.dto.StudioDetailDTO;
import com.wedding.company.repository.CompanyRepository;
import com.wedding.company.repository.DressItemRepository;
import com.wedding.company.repository.HallItemRepository;
import com.wedding.company.repository.MakeupPackageRepository;
import com.wedding.company.service.CompanyService;
import com.wedding.global.util.OpenAiClient;
import com.wedding.openAIClient.repository.ChatMessageRepository;
import com.wedding.openAIClient.domain.ChatMessage;
import com.wedding.openAIClient.dto.ChatReferenceDTO;
import com.wedding.openAIClient.dto.ChatResponseDTO;
import com.wedding.openAIClient.dto.OpenAiMessageDTO;
import com.wedding.openAIClient.dto.OpenAiResponseDTO;
import com.wedding.product.domain.Product;
import com.wedding.product.repository.ProductRepository;
import com.wedding.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final OpenAiClient openAiClient;
    private final ChatMessageRepository chatMessageRepository;
    private final ProductRepository productRepository;
    private final CompanyRepository companyRepository;
    private final CompanyService companyService;
    private final DressItemRepository dressItemRepository;
    private final HallItemRepository hallItemRepository;
    private final MakeupPackageRepository makeupPackageRepository;
    private final ReservationRepository reservationRepository;
    private final ObjectMapper objectMapper;

    // 회원별로 대화를 이만큼만 DB에 남기고, 문맥으로도 이만큼만 사용
    private static final int HISTORY_LIMIT = 20;

    // 한 답변에 카드로 보여줄 참조(업체/아이템/답례품) 최대 개수
    private static final int MAX_REFERENCES = 5;

    // 메이크업 패키지 타입이 포함하는 서비스 조합. HAIR_NAIL을 요청해도 그 조합을 포함하는 더 큰
    // 패키지(FULL)가 있으면 함께 매칭시키기 위해 사용한다
    private static final Map<MakeupPackageType, Set<String>> MAKEUP_PACKAGE_SERVICES = Map.of(
            MakeupPackageType.HAIR, Set.of("HAIR"),
            MakeupPackageType.MAKEUP, Set.of("MAKEUP"),
            MakeupPackageType.NAIL, Set.of("NAIL"),
            MakeupPackageType.HAIR_MAKEUP, Set.of("HAIR", "MAKEUP"),
            MakeupPackageType.HAIR_NAIL, Set.of("HAIR", "NAIL"),
            MakeupPackageType.MAKEUP_NAIL, Set.of("MAKEUP", "NAIL"),
            MakeupPackageType.FULL, Set.of("HAIR", "MAKEUP", "NAIL")
    );

    // 요청한 서비스 조합을 포함하는(상위 호환) 패키지 타입 목록을 반환
    private List<MakeupPackageType> matchingMakeupPackageTypes(MakeupPackageType requested) {
        Set<String> requiredServices = MAKEUP_PACKAGE_SERVICES.get(requested);
        return MAKEUP_PACKAGE_SERVICES.entrySet().stream()
                .filter(e -> e.getValue().containsAll(requiredServices))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    // 함수 실행 결과 - AI에게 줄 텍스트와, 프론트에 카드로 줄 참조 데이터를 같이 담음
    private record ToolResult(String text, List<ChatReferenceDTO> references) {
        private static ToolResult textOnly(String text) {
            return new ToolResult(text, List.of());
        }
    }

    // DB save/delete가 saveAndTrim 안에서 이어지는데, deleteOlderThan은 @Modifying 쿼리라
    // 트랜잭션 없이 호출되면 "Executing an update/delete query" 에러가 남 - 여기서 감싸줌
    @Transactional
    public ChatResponseDTO getAnswer(String memberEmail, String question, String intent, String lockedCategory) {

        // 1) 회원의 최근 대화(최대 HISTORY_LIMIT개, 최신순)를 불러와서 문맥으로 사용할 메시지 목록 구성
        List<ChatMessage> history = chatMessageRepository
                .findRecentByMember(memberEmail, PageRequest.of(0, HISTORY_LIMIT));

        List<OpenAiMessageDTO> messages = new ArrayList<>();
        messages.add(OpenAiMessageDTO.of("system", ChatPrompts.buildSystemPrompt(intent)));

        // findRecentByMember는 최신순(cno desc)으로 오기 때문에, 대화 순서로 쓰려면 뒤집어야 함
        for (int i = history.size() - 1; i >= 0; i--) {
            ChatMessage m = history.get(i);
            messages.add(OpenAiMessageDTO.of(m.getRole(), m.getContent()));
        }

        messages.add(OpenAiMessageDTO.of("user", question));

        // 2) intent가 있으면 그 목적에 맞는 함수(들)만 tools로 전달하고, 없으면(자유 질문) 4개 함수를
        // 다 후보로 준다. tool_choice="required"로 모델이 함수 호출 없이 텍스트로만 답하는 것을 막는다.
        OpenAiResponseDTO firstResponse = openAiClient.getChatCompletions(
                messages, ChatToolDefinitions.resolveTools(intent), "required");

        OpenAiMessageDTO assistantMessage = firstResponse.getChoices().getFirst().getMessage();

        String finalAnswer;
        List<ChatReferenceDTO> references = new ArrayList<>();

        if (assistantMessage.getToolCalls() != null && !assistantMessage.getToolCalls().isEmpty()) {

            // 3) 모델이 함수 호출을 요청한 경우: 실제로 함수를 실행하고, 그 결과를 들고 2차 호출
            List<OpenAiMessageDTO> followUp = new ArrayList<>(messages);
            followUp.add(assistantMessage); // "이 함수 호출할게"라고 한 메시지 그대로 포함시켜야 함

            for (OpenAiMessageDTO.ToolCallDTO toolCall : assistantMessage.getToolCalls()) {

                ToolResult result = executeToolCall(toolCall, lockedCategory);
                references.addAll(result.references());

                followUp.add(OpenAiMessageDTO.builder()
                        .role("tool")
                        .toolCallId(toolCall.getId())
                        .content(result.text())
                        .build());
            }

            OpenAiResponseDTO secondResponse = openAiClient.getChatCompletions(followUp, null);
            finalAnswer = secondResponse.getChoices().getFirst().getMessage().getContent();

        } else {
            // 함수 호출 없이 바로 답변한 경우
            finalAnswer = assistantMessage.getContent();
        }

        // 프롬프트로 "마크다운 쓰지 마라"고 지시해도 모델이 종종 **볼드** 등을 섞어 보내서,
        // 화면에 별표가 그대로 노출되지 않도록 여기서 한 번 더 강제로 걷어낸다
        finalAnswer = stripMarkdown(finalAnswer);

        // 모델이 내부용 숨김 메타데이터 형식을 답변에 그대로 섞어 보내는 경우가 있어 정규식으로 제거한다
        finalAnswer = stripHiddenMetadata(finalAnswer);

        List<ChatReferenceDTO> cappedReferences = references.stream()
                .limit(MAX_REFERENCES)
                .collect(Collectors.toList());

        // 4) DB에는 실제 질문/최종답변 2건만 저장 (함수호출 중간 메시지는 저장 안 함).
        // 화면에 보이는 답변(finalAnswer)은 짧게 유지하되, 다음 턴에서 "그 중 한 업체" 같은
        // 후속 질문을 처리할 수 있도록 이번 턴에 조회된 항목의 id는 보이지 않는 메타데이터로 같이 저장한다.
        saveAndTrim(memberEmail, question, buildHistoryContent(finalAnswer, cappedReferences));

        return ChatResponseDTO.of(finalAnswer, cappedReferences);
    }

    // 화면에는 안 보이지만 다음 턴 문맥으로 넘어갈 때 AI가 이전에 조회한 대상(cmno 등)을 기억할 수 있도록
    // 답변 뒤에 참조 id 목록을 덧붙인다 - saveAndTrim으로 DB에 저장되는 텍스트에만 쓰이고 프론트에는 절대 안 나감
    private String buildHistoryContent(String finalAnswer, List<ChatReferenceDTO> references) {
        if (references.isEmpty()) return finalAnswer;

        String refNote = references.stream()
                .map(r -> String.format("%s[id=%d, 이름=%s]", r.getType(), r.getId(), r.getName()))
                .collect(Collectors.joining(", "));

        return finalAnswer + "\n\n(참고용 메타데이터, 화면에 표시되지 않음: " + refNote + ")";
    }

    // 함수가 늘어날 때마다 이름으로 분기. lockedCategory는 "업체 찾기" 카테고리 버튼으로 고정된 값 -
    // 카테고리를 다루는 함수들에 전달되어 모델이 준 category 값을 덮어쓴다
    private ToolResult executeToolCall(OpenAiMessageDTO.ToolCallDTO toolCall, String lockedCategory) {

        String functionName = toolCall.getFunction().getName();

        return switch (functionName) {
            case "search_gifts" -> executeSearchGifts(toolCall);
            case "search_companies" -> executeSearchCompanies(toolCall, lockedCategory);
            case "get_company_detail" -> executeGetCompanyDetail(toolCall);
            case "find_available_companies" -> executeFindAvailableCompanies(toolCall, lockedCategory);
            case "find_top_items" -> executeFindTopItems(toolCall, lockedCategory);
            // 실제 조회 없이 답할 수 있다고 판단했을 때 호출되는 더미 함수 - 아무 것도 하지 않음
            case "answer_directly" -> ToolResult.textOnly("");
            default -> ToolResult.textOnly("지원하지 않는 함수입니다: " + functionName);
        };
    }

    // 답례품 검색 실행 - tbl_product는 답례품(곡물/식품, 과자/한과, 디퓨저/향수 등) 전용이며 드레스는 없음
    private ToolResult executeSearchGifts(OpenAiMessageDTO.ToolCallDTO toolCall) {

        try {
            JsonNode args = objectMapper.readTree(toolCall.getFunction().getArguments());

            String category = args.hasNonNull("category") ? args.get("category").asText() : null;
            String keyword = args.hasNonNull("keyword") ? args.get("keyword").asText() : null;
            Integer minPrice = args.hasNonNull("minPrice") ? args.get("minPrice").asInt() : null;
            Integer maxPrice = args.hasNonNull("maxPrice") ? args.get("maxPrice").asInt() : null;
            Double minRating = args.hasNonNull("minRating") ? args.get("minRating").asDouble() : null;
            Integer limit = args.hasNonNull("limit") ? args.get("limit").asInt() : null;
            String sortBy = args.hasNonNull("sortBy") ? args.get("sortBy").asText() : null;

            List<String> categories = category != null ? List.of(category) : null;

            // 사용자가 개수를 지정했으면 그 개수를, 아니면 기본 5개까지만
            int effectiveLimit = (limit != null && limit > 0) ? Math.min(limit, MAX_REFERENCES) : MAX_REFERENCES;

            // keyword에 동의어를 쉼표로 여러 개 넣을 수 있음(예: "돌잔치,백일") - 각 키워드로 따로
            // 조회한 뒤 상품번호(pno) 기준으로 중복 없이 합친다
            List<String> keywordTerms = (keyword != null && !keyword.isBlank())
                    ? Arrays.stream(keyword.split(","))
                            .map(String::trim)
                            .filter(k -> !k.isEmpty())
                            .collect(Collectors.toList())
                    : List.of();

            // 정렬은 merge 이후 한 번에 적용하므로, DB 조회 시에는 정렬 없이 후보만 넉넉히 가져온다
            Map<Long, Product> merged = new LinkedHashMap<>();

            if (keywordTerms.isEmpty()) {
                var page = productRepository.searchProductList(
                        categories, null, minPrice, maxPrice, minRating, PageRequest.of(0, 30));
                page.getContent().forEach(row -> {
                    Product p = (Product) row[0];
                    merged.putIfAbsent(p.getPno(), p);
                });
            } else {
                for (String kw : keywordTerms) {
                    var page = productRepository.searchProductList(
                            categories, kw, minPrice, maxPrice, minRating, PageRequest.of(0, 30));
                    page.getContent().forEach(row -> {
                        Product p = (Product) row[0];
                        merged.putIfAbsent(p.getPno(), p);
                    });
                }
            }

            // sortBy 미지정 시 기본값은 평점 높은순
            List<Product> products = new ArrayList<>(merged.values());

            if ("PRICE_DESC".equals(sortBy)) {
                products.sort(Comparator.comparingInt(Product::getPrice).reversed());
            } else if ("PRICE_ASC".equals(sortBy)) {
                products.sort(Comparator.comparingInt(Product::getPrice));
            } else if ("RATING_ASC".equals(sortBy)) {
                products.sort(Comparator.comparingDouble(Product::getRatingAvg));
            } else {
                products.sort(Comparator.comparingDouble(Product::getRatingAvg).reversed());
            }

            products = products.stream().limit(effectiveLimit).collect(Collectors.toList());

            if (products.isEmpty()) {
                return ToolResult.textOnly("조건에 맞는 답례품을 찾지 못했습니다.");
            }

            String text = products.stream()
                    .map(p -> String.format("- %s (%,d원, 카테고리: %s, 평점: %.1f)",
                            p.getPname(), p.getPrice(), p.getCategory(), p.getRatingAvg()))
                    .collect(Collectors.joining("\n"));

            List<ChatReferenceDTO> refs = products.stream()
                    .limit(MAX_REFERENCES)
                    .map(p -> ChatReferenceDTO.builder()
                            .type("GIFT")
                            .id(p.getPno())
                            .name(p.getPname())
                            .imageUrl(productImagePath(p))
                            .priceLabel(String.format("%,d원", p.getPrice()))
                            .link("/product/read/" + p.getPno())
                            .build())
                    .collect(Collectors.toList());

            return new ToolResult(text, refs);

        } catch (Exception e) {
            return ToolResult.textOnly("답례품 검색 중 오류가 발생했습니다.");
        }
    }

    // 업체(홀/드레스업체/스튜디오/메이크업) 검색 실행 - 설명 텍스트를 통째로 넘겨서
    // AI가 느낌/분위기까지 읽고 판단할 수 있게 함
    private ToolResult executeSearchCompanies(OpenAiMessageDTO.ToolCallDTO toolCall, String lockedCategory) {

        try {
            JsonNode args = objectMapper.readTree(toolCall.getFunction().getArguments());

            // 카테고리 버튼으로 고정되어 있으면 모델이 준 category는 무시하고 이 값으로 강제한다
            CompanyCategory category = null;
            if (lockedCategory != null) {
                try {
                    category = CompanyCategory.valueOf(lockedCategory.toUpperCase());
                } catch (IllegalArgumentException ignored) {
                }
            } else if (args.hasNonNull("category")) {
                try {
                    category = CompanyCategory.valueOf(args.get("category").asText().toUpperCase());
                } catch (IllegalArgumentException ignored) {
                    // 모델이 이상한 값을 주면 그냥 카테고리 필터 없이 전체 검색
                }
            }

            String keyword = args.hasNonNull("keyword") ? args.get("keyword").asText() : null;
            BigDecimal minPrice = args.hasNonNull("minPrice") ? BigDecimal.valueOf(args.get("minPrice").asDouble()) : null;
            BigDecimal maxPrice = args.hasNonNull("maxPrice") ? BigDecimal.valueOf(args.get("maxPrice").asDouble()) : null;
            Integer limit = args.hasNonNull("limit") ? args.get("limit").asInt() : null;
            String sortBy = args.hasNonNull("sortBy") ? args.get("sortBy").asText() : null;

            // 사용자가 개수를 지정했으면 그 개수를, 아니면 기본 5개까지만
            int effectiveLimit = (limit != null && limit > 0) ? Math.min(limit, MAX_REFERENCES) : MAX_REFERENCES;

            // 카테고리별 실제 보유 옵션/상품 필터 - 지정되면 keyword 기반 설명 검색보다 우선한다
            Set<Long> itemFilteredCmnos = null;

            if (args.hasNonNull("dressItemType")) {
                try {
                    DressItemType type = DressItemType.valueOf(args.get("dressItemType").asText().toUpperCase());
                    itemFilteredCmnos = new HashSet<>(dressItemRepository.findCompanyCmnosByItemType(type));
                } catch (IllegalArgumentException ignored) {
                    // 모델이 이상한 값을 주면 이 필터는 무시하고 keyword 검색으로 처리
                }
            } else if (args.hasNonNull("hallMealType")) {
                try {
                    MealType type = MealType.valueOf(args.get("hallMealType").asText().toUpperCase());
                    itemFilteredCmnos = new HashSet<>(hallItemRepository.findCompanyCmnosByMealType(type));
                } catch (IllegalArgumentException ignored) {
                }
            } else if (args.hasNonNull("makeupPackageType")) {
                try {
                    MakeupPackageType type = MakeupPackageType.valueOf(args.get("makeupPackageType").asText().toUpperCase());
                    itemFilteredCmnos = new HashSet<>(makeupPackageRepository
                            .findCompanyCmnosByPackageTypeIn(matchingMakeupPackageTypes(type)));
                } catch (IllegalArgumentException ignored) {
                }
            }

            // keyword에 동의어를 쉼표로 여러 개 넣을 수 있음(예: "슈트,턱시도,정장") - 각 키워드로
            // 따로 조회한 뒤 업체번호(cmno) 기준으로 중복 없이 합친다
            List<String> keywordTerms = (keyword != null && !keyword.isBlank())
                    ? Arrays.stream(keyword.split(","))
                            .map(String::trim)
                            .filter(k -> !k.isEmpty())
                            .collect(Collectors.toList())
                    : List.of();

            // searchListWithDescription은 업체명/주소뿐 아니라 설명(description) 텍스트까지 검색한다
            Map<Long, Company> merged = new LinkedHashMap<>();

            if (itemFilteredCmnos != null) {
                // 람다 캡처를 위해 effectively final 변수로 복사
                final CompanyCategory categoryFilter = category;
                for (Long cmno : itemFilteredCmnos) {
                    companyRepository.selectOne(cmno).ifPresent(c -> {
                        boolean categoryOk = categoryFilter == null || c.getCategory() == categoryFilter;
                        boolean minOk = minPrice == null || c.getPriceAvg().compareTo(minPrice) >= 0;
                        boolean maxOk = maxPrice == null || c.getPriceAvg().compareTo(maxPrice) <= 0;
                        if (categoryOk && minOk && maxOk) {
                            merged.putIfAbsent(cmno, c);
                        }
                    });
                }
            } else {
                // category가 없으면(카테고리 버튼도 안 거치고 자유질문으로 막연히 물은 경우) 한쪽
                // 카테고리로 쏠리지 않도록 4개 카테고리 모두에서 고르게 가져온다
                List<CompanyCategory> categoriesToQuery = category != null
                        ? List.of(category)
                        : Arrays.asList(CompanyCategory.values());
                int perCategoryLimit = category != null ? 15 : 4;

                if (keywordTerms.isEmpty()) {
                    for (CompanyCategory cat : categoriesToQuery) {
                        var page = companyRepository.searchListWithDescription(
                                cat, null, minPrice, maxPrice, PageRequest.of(0, perCategoryLimit));
                        page.getContent().forEach(c -> merged.putIfAbsent(c.getCmno(), c));
                    }
                } else {
                    for (String kw : keywordTerms) {
                        for (CompanyCategory cat : categoriesToQuery) {
                            var page = companyRepository.searchListWithDescription(
                                    cat, kw, minPrice, maxPrice, PageRequest.of(0, perCategoryLimit));
                            page.getContent().forEach(c -> merged.putIfAbsent(c.getCmno(), c));
                        }
                    }
                }
            }

            List<Company> companies = new ArrayList<>(merged.values());

            if ("PRICE_DESC".equals(sortBy)) {
                companies.sort(Comparator.comparing(Company::getPriceAvg, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
            } else if ("PRICE_ASC".equals(sortBy)) {
                companies.sort(Comparator.comparing(Company::getPriceAvg, Comparator.nullsLast(Comparator.naturalOrder())));
            }

            companies = companies.stream()
                    .limit(effectiveLimit)
                    .collect(Collectors.toList());

            if (companies.isEmpty()) {
                return ToolResult.textOnly("조건에 맞는 업체를 찾지 못했습니다.");
            }

            // cmno를 결과에 포함시켜서, AI가 이어서 get_company_detail(cmno)를 호출할 수 있게 함
            String text = companies.stream()
                    .map(c -> String.format(
                            "- %s (%s, 업체번호 %d) | 주소: %s | 평균가격: %s\n  설명: %s",
                            c.getName(), c.getCategory(), c.getCmno(), c.getAddress(),
                            formatPrice(c.getPriceAvg()),
                            c.getDescription() != null ? c.getDescription() : "설명 없음"))
                    .collect(Collectors.joining("\n"));

            List<ChatReferenceDTO> refs = companies.stream()
                    .map(c -> ChatReferenceDTO.builder()
                            .type("COMPANY")
                            .id(c.getCmno())
                            .name(c.getName())
                            .imageUrl(companyImagePath(c))
                            .priceLabel(formatPrice(c.getPriceAvg()))
                            .link("/companies/read/" + c.getCmno())
                            .build())
                    .collect(Collectors.toList());

            return new ToolResult(text, refs);

        } catch (Exception e) {
            return ToolResult.textOnly("업체 검색 중 오류가 발생했습니다.");
        }
    }

    // 업체 하나의 세부 상품/옵션 조회 - 카테고리별로 실제 하위 데이터가 다르므로 분기해서 포맷팅 + 카드 생성
    private ToolResult executeGetCompanyDetail(OpenAiMessageDTO.ToolCallDTO toolCall) {

        try {
            JsonNode args = objectMapper.readTree(toolCall.getFunction().getArguments());

            if (!args.hasNonNull("cmno")) {
                return ToolResult.textOnly("업체번호(cmno)가 필요합니다. 먼저 search_companies로 업체를 찾아주세요.");
            }

            Long cmno = args.get("cmno").asLong();
            CompanyDTO company = companyService.get(cmno);

            if (company == null) {
                return ToolResult.textOnly("해당 업체번호의 업체를 찾을 수 없습니다.");
            }

            // 텍스트와 카드는 항상 같은 (필터 + 정렬 + 개수제한된) 목록에서 만든다
            String sortBy = args.hasNonNull("sortBy") ? args.get("sortBy").asText() : null;
            Integer limit = args.hasNonNull("limit") ? args.get("limit").asInt() : null;
            int effectiveLimit = (limit != null && limit > 0) ? Math.min(limit, MAX_REFERENCES) : MAX_REFERENCES;

            // 카테고리별 타입 필터 - "이 업체 슈트만 보여줘"처럼 특정 타입만 보고 싶어할 때 사용
            DressItemType dressItemTypeFilter = null;
            if (args.hasNonNull("dressItemType")) {
                try {
                    dressItemTypeFilter = DressItemType.valueOf(args.get("dressItemType").asText().toUpperCase());
                } catch (IllegalArgumentException ignored) {
                }
            }
            MealType hallMealTypeFilter = null;
            if (args.hasNonNull("hallMealType")) {
                try {
                    hallMealTypeFilter = MealType.valueOf(args.get("hallMealType").asText().toUpperCase());
                } catch (IllegalArgumentException ignored) {
                }
            }
            List<MakeupPackageType> makeupPackageTypeFilter = null;
            if (args.hasNonNull("makeupPackageType")) {
                try {
                    makeupPackageTypeFilter = matchingMakeupPackageTypes(
                            MakeupPackageType.valueOf(args.get("makeupPackageType").asText().toUpperCase()));
                } catch (IllegalArgumentException ignored) {
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%s (%s) | 주소: %s | 평균가격: %s\n",
                    company.getName(), company.getCategory(), company.getAddress(),
                    formatPrice(company.getPriceAvg())));

            List<ChatReferenceDTO> refs = new ArrayList<>();

            switch (company.getCategory()) {
                case DRESS -> {
                    DressDetailDTO detail = company.getDressDetail();
                    if (detail == null || detail.getItems() == null || detail.getItems().isEmpty()) {
                        sb.append("등록된 드레스 아이템이 없습니다.");
                    } else {
                        List<DressItemDTO> items = new ArrayList<>(detail.getItems());
                        if (dressItemTypeFilter != null) {
                            final DressItemType typeFilter = dressItemTypeFilter;
                            items = items.stream()
                                    .filter(item -> item.getItemType() == typeFilter)
                                    .collect(Collectors.toList());
                        }

                        if (items.isEmpty()) {
                            sb.append("해당 타입의 드레스 아이템이 없습니다.");
                        } else {
                        if ("PRICE_DESC".equals(sortBy)) {
                            items.sort(Comparator.comparing(DressItemDTO::getPrice, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
                        } else if ("PRICE_ASC".equals(sortBy)) {
                            items.sort(Comparator.comparing(DressItemDTO::getPrice, Comparator.nullsLast(Comparator.naturalOrder())));
                        }
                        items = items.stream().limit(effectiveLimit).collect(Collectors.toList());

                        items.forEach(item -> sb.append(String.format(
                                "- %s (%,d원, 타입: %s, 스타일: %s, 사이즈: %s)\n",
                                item.getItemName(),
                                item.getPrice() != null ? item.getPrice().longValue() : 0L,
                                item.getItemType(),
                                item.getStyleTags() != null ? item.getStyleTags() : "정보없음",
                                item.getSizeRange() != null ? item.getSizeRange() : "정보없음")));

                        // 드레스 아이템은 별도 상세페이지(dress-items/read)가 실제로는 안 쓰이는
                        // 페이지라서, 홀/메이크업과 동일하게 업체 페이지로 이동시킨다
                        items.forEach(item ->
                                refs.add(ChatReferenceDTO.builder()
                                        .type("DRESS_ITEM")
                                        .id(item.getDressItemId())
                                        .name(item.getItemName())
                                        .imageUrl(itemImagePath(item.getImageUrl()))
                                        .priceLabel(item.getPrice() != null
                                                ? String.format("%,d원", item.getPrice().longValue())
                                                : "가격 정보없음")
                                        .link("/companies/read/" + cmno)
                                        .build()));
                        }
                    }
                }
                case HALL -> {
                    HallDetailDTO detail = company.getHallDetail();
                    if (detail == null || detail.getItems() == null || detail.getItems().isEmpty()) {
                        sb.append("등록된 홀 대관 옵션이 없습니다.");
                    } else {
                        List<HallItemDTO> items = new ArrayList<>(detail.getItems());
                        if (hallMealTypeFilter != null) {
                            final MealType typeFilter = hallMealTypeFilter;
                            items = items.stream()
                                    .filter(item -> item.getMealType() == typeFilter)
                                    .collect(Collectors.toList());
                        }

                        if (items.isEmpty()) {
                            sb.append("해당 식사 타입의 대관 옵션이 없습니다.");
                        } else {
                            if ("PRICE_DESC".equals(sortBy)) {
                                items.sort(Comparator.comparing(HallItemDTO::getPrice, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
                            } else if ("PRICE_ASC".equals(sortBy)) {
                                items.sort(Comparator.comparing(HallItemDTO::getPrice, Comparator.nullsLast(Comparator.naturalOrder())));
                            }
                            items = items.stream().limit(effectiveLimit).collect(Collectors.toList());

                            items.forEach(item -> sb.append(String.format(
                                    "- %s (%,d원, 수용인원: %s명, 식사타입: %s)\n",
                                    item.getItemName(),
                                    item.getPrice() != null ? item.getPrice().longValue() : 0L,
                                    item.getCapacity() != null ? item.getCapacity() : "정보없음",
                                    item.getMealType())));

                            // 홀 옵션은 별도 상세페이지가 없어서 업체 페이지로 이동시킴
                            items.forEach(item ->
                                    refs.add(ChatReferenceDTO.builder()
                                            .type("HALL_ITEM")
                                            .id(item.getHallItemId())
                                            .name(item.getItemName())
                                            .imageUrl(itemImagePath(item.getImageUrl()))
                                            .priceLabel(item.getPrice() != null
                                                    ? String.format("%,d원", item.getPrice().longValue())
                                                    : "가격 정보없음")
                                            .link("/companies/read/" + cmno)
                                            .build()));
                        }
                    }
                }
                case MAKEUP -> {
                    MakeupDetailDTO detail = company.getMakeupDetail();
                    if (detail == null || detail.getPackages() == null || detail.getPackages().isEmpty()) {
                        sb.append("등록된 메이크업 패키지가 없습니다.");
                    } else {
                        List<MakeupPackageDTO> packages = new ArrayList<>(detail.getPackages());
                        if (makeupPackageTypeFilter != null) {
                            final List<MakeupPackageType> typeFilter = makeupPackageTypeFilter;
                            packages = packages.stream()
                                    .filter(pkg -> typeFilter.contains(pkg.getPackageType()))
                                    .collect(Collectors.toList());
                        }

                        if (packages.isEmpty()) {
                            sb.append("해당 타입의 메이크업 패키지가 없습니다.");
                        } else {
                        // 메이크업 패키지는 절대가격이 없어(할인율만 존재) sortBy를 적용하지 않지만,
                        // 텍스트/카드가 서로 다른 개수로 어긋나지 않도록 개수 제한은 동일하게 맞춘다
                        packages = packages.stream()
                                .limit(effectiveLimit)
                                .collect(Collectors.toList());

                        packages.forEach(pkg -> sb.append(String.format(
                                "- %s 패키지 (할인율 %s%%)\n",
                                pkg.getPackageType(),
                                pkg.getDiscountRate() != null ? pkg.getDiscountRate().toPlainString() : "0")));

                        // 메이크업 패키지도 별도 상세페이지가 없어서 업체 페이지로 이동시킴
                        packages.forEach(pkg ->
                                refs.add(ChatReferenceDTO.builder()
                                        .type("MAKEUP_PACKAGE")
                                        .id(pkg.getPackageId())
                                        .name(pkg.getPackageType() + " 패키지")
                                        .imageUrl(null)
                                        .priceLabel("할인율 " + (pkg.getDiscountRate() != null
                                                ? pkg.getDiscountRate().toPlainString() : "0") + "%")
                                        .link("/companies/read/" + cmno)
                                        .build()));
                        }
                    }
                }
                case STUDIO -> {
                    StudioDetailDTO detail = company.getStudioDetail();
                    sb.append("이 업체는 세부 상품 목록이 없고, 테마 태그만 등록되어 있습니다: ")
                            .append(detail != null && detail.getThemeTags() != null ? detail.getThemeTags() : "정보없음");
                }
            }

            // 업체 자체 카드도 맨 앞에 하나 포함
            refs.add(0, ChatReferenceDTO.builder()
                    .type("COMPANY")
                    .id(cmno)
                    .name(company.getName())
                    .imageUrl(companyImagePath(company))
                    .priceLabel(formatPrice(company.getPriceAvg()))
                    .link("/companies/read/" + cmno)
                    .build());

            return new ToolResult(sb.toString(), refs);

        } catch (Exception e) {
            return ToolResult.textOnly("업체 상세 조회 중 오류가 발생했습니다.");
        }
    }

    // 날짜+카테고리(+지역/가격/옵션) 기반 예약 가능 업체 조회 - 그 날짜에 예약 기록이 하나도 없는
    // 업체만 반환한다 (옵션 단위가 아니라 업체 단위 기준)
    private ToolResult executeFindAvailableCompanies(OpenAiMessageDTO.ToolCallDTO toolCall, String lockedCategory) {

        try {
            JsonNode args = objectMapper.readTree(toolCall.getFunction().getArguments());

            if (lockedCategory == null && !args.hasNonNull("category")) {
                return ToolResult.textOnly("예약 가능 여부를 확인하려면 업체 종류가 필요합니다.");
            }
            if (!args.hasNonNull("date")) {
                return ToolResult.textOnly("예약 가능 여부를 확인하려면 날짜가 필요합니다.");
            }

            // 카테고리 버튼으로 고정되어 있으면 모델이 준 category는 무시하고 이 값으로 강제한다
            CompanyCategory category;
            try {
                category = CompanyCategory.valueOf(
                        (lockedCategory != null ? lockedCategory : args.get("category").asText()).toUpperCase());
            } catch (IllegalArgumentException e) {
                return ToolResult.textOnly("업체 종류를 이해하지 못했습니다.");
            }

            LocalDate date;
            try {
                date = LocalDate.parse(args.get("date").asText());
            } catch (Exception e) {
                return ToolResult.textOnly("날짜 형식을 이해하지 못했습니다.");
            }

            String location = args.hasNonNull("location") ? args.get("location").asText() : null;
            BigDecimal minPrice = args.hasNonNull("minPrice") ? BigDecimal.valueOf(args.get("minPrice").asDouble()) : null;
            BigDecimal maxPrice = args.hasNonNull("maxPrice") ? BigDecimal.valueOf(args.get("maxPrice").asDouble()) : null;
            String sortBy = args.hasNonNull("sortBy") ? args.get("sortBy").asText() : null;
            Integer limit = args.hasNonNull("limit") ? args.get("limit").asInt() : null;
            int effectiveLimit = (limit != null && limit > 0) ? Math.min(limit, MAX_REFERENCES) : MAX_REFERENCES;

            // 실제 보유 옵션 필터가 지정되면 그걸로 후보를 좁힌다
            Set<Long> itemFilteredCmnos = null;
            if (args.hasNonNull("dressItemType")) {
                try {
                    DressItemType type = DressItemType.valueOf(args.get("dressItemType").asText().toUpperCase());
                    itemFilteredCmnos = new HashSet<>(dressItemRepository.findCompanyCmnosByItemType(type));
                } catch (IllegalArgumentException ignored) {
                }
            } else if (args.hasNonNull("hallMealType")) {
                try {
                    MealType type = MealType.valueOf(args.get("hallMealType").asText().toUpperCase());
                    itemFilteredCmnos = new HashSet<>(hallItemRepository.findCompanyCmnosByMealType(type));
                } catch (IllegalArgumentException ignored) {
                }
            } else if (args.hasNonNull("makeupPackageType")) {
                try {
                    MakeupPackageType type = MakeupPackageType.valueOf(args.get("makeupPackageType").asText().toUpperCase());
                    itemFilteredCmnos = new HashSet<>(makeupPackageRepository
                            .findCompanyCmnosByPackageTypeIn(matchingMakeupPackageTypes(type)));
                } catch (IllegalArgumentException ignored) {
                }
            }

            List<Company> candidates;

            if (itemFilteredCmnos != null) {
                candidates = new ArrayList<>();
                for (Long cmno : itemFilteredCmnos) {
                    companyRepository.selectOne(cmno).ifPresent(c -> {
                        boolean categoryOk = c.getCategory() == category;
                        boolean locationOk = location == null || location.isBlank()
                                || (c.getAddress() != null && c.getAddress().contains(location));
                        boolean minOk = minPrice == null || c.getPriceAvg().compareTo(minPrice) >= 0;
                        boolean maxOk = maxPrice == null || c.getPriceAvg().compareTo(maxPrice) <= 0;
                        if (categoryOk && locationOk && minOk && maxOk) {
                            candidates.add(c);
                        }
                    });
                }
            } else {
                // 후보를 30개까지 넉넉히 가져온 뒤, 그 날짜에 예약이 없는 곳만 걸러서 effectiveLimit개로 맞춘다
                var page = companyRepository.searchList(category, location, minPrice, maxPrice, PageRequest.of(0, 30));
                candidates = new ArrayList<>(page.getContent());
            }

            List<Company> available = candidates.stream()
                    .filter(c -> !reservationRepository.existsByCmnoAndWeddingDate(c.getCmno(), date))
                    .collect(Collectors.toList());

            // "예약 가능한 곳 중 가장 저렴한 곳"처럼 순위 질문에 대응
            if ("PRICE_DESC".equals(sortBy)) {
                available.sort(Comparator.comparing(Company::getPriceAvg, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
            } else if ("PRICE_ASC".equals(sortBy)) {
                available.sort(Comparator.comparing(Company::getPriceAvg, Comparator.nullsLast(Comparator.naturalOrder())));
            }

            available = available.stream().limit(effectiveLimit).collect(Collectors.toList());

            if (available.isEmpty()) {
                return ToolResult.textOnly("해당 조건에 예약 가능한 업체를 찾지 못했습니다.");
            }

            String text = available.stream()
                    .map(c -> String.format(
                            "- %s (%s, 업체번호 %d) | 주소: %s | 평균가격: %s",
                            c.getName(), c.getCategory(), c.getCmno(), c.getAddress(), formatPrice(c.getPriceAvg())))
                    .collect(Collectors.joining("\n"));

            List<ChatReferenceDTO> refs = available.stream()
                    .map(c -> ChatReferenceDTO.builder()
                            .type("COMPANY")
                            .id(c.getCmno())
                            .name(c.getName())
                            .imageUrl(companyImagePath(c))
                            .priceLabel(formatPrice(c.getPriceAvg()))
                            // 검색 결과가 아니라 "예약 가능 업체" 조회 결과이므로, 상세페이지가 아니라
                            // 바로 예약 페이지로 이동시킨다
                            .link("/companies/reserve/" + c.getCmno())
                            .build())
                    .collect(Collectors.toList());

            return new ToolResult(text, refs);

        } catch (Exception e) {
            return ToolResult.textOnly("예약 가능 업체 조회 중 오류가 발생했습니다.");
        }
    }

    // 개별 드레스 상품/홀 대관 옵션을 실제 등록 가격 기준으로 순위 매겨 조회 - search_companies의
    // sortBy(업체 평균가격)와 달리, tbl_dress_item/tbl_hall_item의 실제 price로 정렬한다.
    // MAKEUP은 패키지에 절대가격이 없어(discountRate만 존재) 지원하지 않는다
    private ToolResult executeFindTopItems(OpenAiMessageDTO.ToolCallDTO toolCall, String lockedCategory) {

        try {
            JsonNode args = objectMapper.readTree(toolCall.getFunction().getArguments());

            if (lockedCategory == null && !args.hasNonNull("category")) {
                return ToolResult.textOnly("카테고리가 필요합니다.");
            }
            if (!args.hasNonNull("sortBy")) {
                return ToolResult.textOnly("정렬 기준이 필요합니다.");
            }

            // 카테고리 버튼으로 고정되어 있으면 모델이 준 category는 무시하고 이 값으로 강제한다
            CompanyCategory category;
            try {
                category = CompanyCategory.valueOf(
                        (lockedCategory != null ? lockedCategory : args.get("category").asText()).toUpperCase());
            } catch (IllegalArgumentException e) {
                return ToolResult.textOnly("카테고리를 이해하지 못했습니다.");
            }

            if (category == CompanyCategory.MAKEUP) {
                return ToolResult.textOnly("메이크업 패키지는 절대가격이 아니라 할인율 기준이라 가격 순위를 매길 수 없습니다.");
            }
            if (category == CompanyCategory.STUDIO) {
                return ToolResult.textOnly("스튜디오는 개별 상품 데이터가 없어 가격 순위를 매길 수 없습니다.");
            }

            boolean ascending = "PRICE_ASC".equals(args.get("sortBy").asText());
            Sort sort = ascending ? Sort.by("price").ascending() : Sort.by("price").descending();

            Integer limit = args.hasNonNull("limit") ? args.get("limit").asInt() : null;
            int effectiveLimit = (limit != null && limit > 0) ? Math.min(limit, MAX_REFERENCES) : 1;

            String text;
            List<ChatReferenceDTO> refs;

            if (category == CompanyCategory.DRESS) {
                List<DressItem> items = dressItemRepository.findAllActive(PageRequest.of(0, effectiveLimit, sort));

                if (items.isEmpty()) {
                    return ToolResult.textOnly("등록된 드레스 상품을 찾지 못했습니다.");
                }

                text = items.stream()
                        .map(item -> String.format("- %s (%,d원, 업체: %s)",
                                item.getItemName(),
                                item.getPrice() != null ? item.getPrice().longValue() : 0L,
                                item.getCompany().getName()))
                        .collect(Collectors.joining("\n"));

                // 드레스 아이템도 홀 옵션과 동일하게 별도 상세페이지가 없으므로 업체 페이지로 이동
                refs = items.stream()
                        .map(item -> ChatReferenceDTO.builder()
                                .type("DRESS_ITEM")
                                .id(item.getDressItemId())
                                .name(item.getItemName() + " (" + item.getCompany().getName() + ")")
                                .imageUrl(itemImagePath(item.getImageUrl()))
                                .priceLabel(item.getPrice() != null
                                        ? String.format("%,d원", item.getPrice().longValue())
                                        : "가격 정보없음")
                                .link("/companies/read/" + item.getCompany().getCmno())
                                .build())
                        .collect(Collectors.toList());
            } else {
                List<HallItem> items = hallItemRepository.findAllActive(PageRequest.of(0, effectiveLimit, sort));

                if (items.isEmpty()) {
                    return ToolResult.textOnly("등록된 홀 대관 옵션을 찾지 못했습니다.");
                }

                text = items.stream()
                        .map(item -> String.format("- %s (%,d원, 업체: %s)",
                                item.getItemName(),
                                item.getPrice() != null ? item.getPrice().longValue() : 0L,
                                item.getCompany().getName()))
                        .collect(Collectors.joining("\n"));

                // 홀 옵션은 별도 상세페이지가 없어서 업체 페이지로 이동시킴
                refs = items.stream()
                        .map(item -> ChatReferenceDTO.builder()
                                .type("HALL_ITEM")
                                .id(item.getHallItemId())
                                .name(item.getItemName() + " (" + item.getCompany().getName() + ")")
                                .imageUrl(itemImagePath(item.getImageUrl()))
                                .priceLabel(item.getPrice() != null
                                        ? String.format("%,d원", item.getPrice().longValue())
                                        : "가격 정보없음")
                                .link("/companies/read/" + item.getCompany().getCmno())
                                .build())
                        .collect(Collectors.toList());
            }

            return new ToolResult(text, refs);

        } catch (Exception e) {
            return ToolResult.textOnly("상품/옵션 순위 조회 중 오류가 발생했습니다.");
        }
    }

    // 드레스 아이템 styleTags에 실제 쓰이는 어휘로 한정해야 사진 분석 결과와 매칭이 된다.
    // 2026-07-18 기준 dress.json에 실제 등록된 값 - 새 태그가 추가되면 이 목록도 갱신해야 함
    private static final String DRESS_STYLE_TAG_VOCABULARY =
            "A라인, 머메이드, 미니드레스, 벨라인, 앵클라인, 오프숄더, 실크, 수입실크, 비즈, 미니멀, " +
                    "셀프웨딩, 스몰웨딩, 본식, 파티, 화동, 우아한, 글래머러스, 화려한, 클래식, 모던, " +
                    "청순, 로맨틱";

    // 드레스 사진 업로드 → 유사 아이템 추천 - 버튼으로 목적이 이미 고정된 흐름이라, 함수 판단 없이
    // "사진 분석 → 스타일 키워드 → DB 검색" 순서로 바로 처리한다 (getAnswer의 함수호출 파이프라인 안 씀)
    @Transactional
    public ChatResponseDTO recommendSimilarDresses(String memberEmail, MultipartFile photo) {

        String styleKeyword;
        try {
            styleKeyword = openAiClient.describeImage(
                    photo.getBytes(),
                    photo.getContentType(),
                    "이 사진 속 드레스를 분석해서 아래 목록에서만 키워드를 골라줘: " +
                            DRESS_STYLE_TAG_VOCABULARY + ". 먼저 드레스의 실루엣(A라인/머메이드/미니드레스/" +
                            "벨라인/앵클라인/오프숄더 등)을 사진을 보고 정확히 판단해서 반드시 1개 포함하고, " +
                            "그다음 분위기나 소재 키워드를 1~4개 더 골라줘. 목록에 있는 단어만 정확히 그대로, " +
                            "다른 설명 없이 쉼표로 나열해줘.");
        } catch (IOException e) {
            return ChatResponseDTO.of("사진을 읽는 중 문제가 발생했습니다. 다시 시도해주세요.", List.of());
        } catch (Exception e) {
            return ChatResponseDTO.of("사진 분석 중 오류가 발생했습니다. 다시 시도해주세요.", List.of());
        }

        // 여러 키워드가 쉼표로 올 수 있어(예: "머메이드, 우아한, 화려한") 하나로 뭉쳐서 검색하면
        // 거의 매칭이 안 된다 - 각 키워드로 따로 조회한 뒤 아이템 기준으로 합친다
        List<String> keywordTerms = Arrays.stream(styleKeyword.split(","))
                .map(String::trim)
                .filter(k -> !k.isEmpty())
                .collect(Collectors.toList());

        // 몇 개의 키워드와 겹치는지도 같이 세어서, 사진마다 실제로 가장 잘 맞는 아이템이 상위에
        // 오도록 한다 (안 그러면 흔한 태그 하나만으로도 항상 같은 상위 몇 개가 고정되어 나옴)
        Map<Long, DressItem> itemsById = new LinkedHashMap<>();
        Map<Long, Integer> matchCount = new HashMap<>();
        for (String kw : keywordTerms) {
            for (DressItem item : dressItemRepository.searchByStyleKeyword(kw, null, null)) {
                itemsById.putIfAbsent(item.getDressItemId(), item);
                matchCount.merge(item.getDressItemId(), 1, Integer::sum);
            }
        }
        // matchCount가 같은 아이템끼리는 매번 같은 순서로 고정되지 않도록 무작위 순번을 함께 매긴다
        Map<Long, Double> tieBreaker = new HashMap<>();
        List<DressItem> items = itemsById.values().stream()
                .sorted(Comparator
                        .comparingInt((DressItem item) -> matchCount.get(item.getDressItemId()))
                        .reversed()
                        .thenComparing(item -> tieBreaker.computeIfAbsent(item.getDressItemId(), id -> Math.random())))
                .collect(Collectors.toList());

        String answer;
        List<ChatReferenceDTO> refs;

        if (items.isEmpty()) {
            answer = "사진에서 \"" + styleKeyword + "\" 느낌을 읽었지만, 비슷한 드레스를 찾지 못했습니다.";
            refs = List.of();
        } else {
            answer = "사진에서 \"" + styleKeyword + "\" 느낌을 읽었어요. 비슷한 분위기의 드레스를 찾아봤습니다.";
            // 드레스 아이템 상세페이지가 실제로는 안 쓰이므로 업체 페이지로 이동시킨다
            refs = items.stream()
                    .limit(MAX_REFERENCES)
                    .map(item -> ChatReferenceDTO.builder()
                            .type("DRESS_ITEM")
                            .id(item.getDressItemId())
                            .name(item.getItemName())
                            .imageUrl(itemImagePath(item.getImageUrl()))
                            .priceLabel(item.getPrice() != null
                                    ? String.format("%,d원", item.getPrice().longValue())
                                    : "가격 정보없음")
                            .link("/companies/read/" + item.getCompany().getCmno())
                            .build())
                    .collect(Collectors.toList());
        }

        // 대화 이력에도 남겨서, 다음 턴에 "그 중 하나 더 자세히" 같은 후속 질문의 문맥이 되게 한다
        saveAndTrim(memberEmail, "[드레스 사진을 올려서 비슷한 아이템 추천을 요청함]",
                buildHistoryContent(answer, refs));

        return ChatResponseDTO.of(answer, refs);
    }

    // 프롬프트로 지시해도 모델이 가끔 마크다운을 섞어 보내므로, 화면(일반 텍스트 채팅창)에 기호가
    // 그대로 노출되지 않도록 흔한 마크다운 문법을 걷어낸다 - 목록에 쓰는 "- "는 건드리지 않음
    private String stripMarkdown(String text) {
        if (text == null) return null;

        String result = text;
        result = result.replaceAll("\\*\\*(.+?)\\*\\*", "$1"); // **볼드**
        result = result.replaceAll("__(.+?)__", "$1");         // __볼드__
        result = result.replaceAll("(?m)^\\s*#{1,6}\\s*", ""); // # 제목
        result = result.replaceAll("```", "");                 // 코드블록 펜스
        result = result.replaceAll("`([^`]*)`", "$1");         // `인라인 코드`
        result = result.replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1"); // [텍스트](URL)

        return result;
    }

    // 프롬프트로 지시해도 모델이 내부용 메타데이터 형식("(참고용 메타데이터, 화면에 표시되지
    // 않음: ...)")을 답변 끝에 그대로 섞어 보내는 경우가 있어, stripMarkdown과 마찬가지로
    // 화면 노출 전에 정규식으로 한 번 더 제거한다
    private String stripHiddenMetadata(String text) {
        if (text == null) return null;
        return text.replaceAll("(?s)\\n*\\(참고용 메타데이터,?\\s*화면에 표시되지 않음:.*?\\)\\s*$", "").trim();
    }

    // 업체 평균가격(BigDecimal, scale 2)을 "11,000,000원" 형태로 통일 - toPlainString()을 쓰면
    // "11000000.00"처럼 소수점이 그대로 노출돼서 longValue()로 소수부를 버리고 콤마 포맷팅함
    private String formatPrice(BigDecimal price) {
        if (price == null) return "가격 정보없음";
        return String.format("%,d원", price.longValue());
    }

    // 업체 엔티티에서 대표 이미지 경로 추출 (검색 결과용)
    private String companyImagePath(Company company) {
        return company.getImageList().stream()
                .findFirst()
                .map(img -> "/api/companies/images/view/" + img.getFileName())
                .orElse(null);
    }

    // 업체 DTO에서 대표 이미지 경로 추출 (업체 상세 조회용)
    private String companyImagePath(CompanyDTO company) {
        if (company.getUploadFileNames() == null || company.getUploadFileNames().isEmpty()) {
            return null;
        }
        return "/api/companies/images/view/" + company.getUploadFileNames().get(0);
    }

    // 답례품 엔티티에서 대표 이미지 경로 추출
    private String productImagePath(Product product) {
        return product.getImageList().stream()
                .findFirst()
                .map(img -> "/api/product/view/" + img.getFileName())
                .orElse(null);
    }

    // 드레스 아이템/홀 옵션의 imageUrl 필드는 파일명만 저장돼 있다고 가정하고 회사 이미지 경로로 감싸줌.
    // 이미 절대/상대 URL 형태로 저장돼 있으면 그대로 사용.
    private String itemImagePath(String fileName) {
        if (fileName == null || fileName.isBlank()) return null;
        if (fileName.startsWith("http") || fileName.startsWith("/")) return fileName;
        return "/api/companies/images/view/" + fileName;
    }

    // 질문/최종답변을 DB에 저장하고, 회원당 HISTORY_LIMIT개를 넘으면 오래된 것부터 삭제
    private void saveAndTrim(String memberEmail, String question, String answer) {

        chatMessageRepository.save(
                ChatMessage.builder().memberEmail(memberEmail).role("user").content(question).build());
        chatMessageRepository.save(
                ChatMessage.builder().memberEmail(memberEmail).role("assistant").content(answer).build());

        List<ChatMessage> recent = chatMessageRepository
                .findRecentByMember(memberEmail, PageRequest.of(0, HISTORY_LIMIT));

        if (recent.size() == HISTORY_LIMIT) {
            Long oldestCnoToKeep = recent.get(recent.size() - 1).getCno();
            chatMessageRepository.deleteOlderThan(memberEmail, oldestCnoToKeep);
        }
    }

}