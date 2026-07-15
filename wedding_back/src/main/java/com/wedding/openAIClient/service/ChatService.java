package com.wedding.openAIClient.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedding.company.domain.Company;
import com.wedding.company.domain.CompanyCategory;
import com.wedding.company.dto.CompanyDTO;
import com.wedding.company.dto.DressDetailDTO;
import com.wedding.company.dto.HallDetailDTO;
import com.wedding.company.dto.MakeupDetailDTO;
import com.wedding.company.dto.StudioDetailDTO;
import com.wedding.company.repository.CompanyRepository;
import com.wedding.company.service.CompanyService;
import com.wedding.global.util.OpenAiClient;
import com.wedding.openAIClient.controller.ChatMessageRepository;
import com.wedding.openAIClient.domain.ChatMessage;
import com.wedding.openAIClient.dto.ChatReferenceDTO;
import com.wedding.openAIClient.dto.ChatResponseDTO;
import com.wedding.openAIClient.dto.OpenAiMessageDTO;
import com.wedding.openAIClient.dto.OpenAiResponseDTO;
import com.wedding.openAIClient.dto.OpenAiToolDTO;
import com.wedding.product.domain.Product;
import com.wedding.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final OpenAiClient openAiClient;
    private final ChatMessageRepository chatMessageRepository;
    private final ProductRepository productRepository;
    private final CompanyRepository companyRepository;
    private final CompanyService companyService;
    private final ObjectMapper objectMapper;

    // 회원별로 대화를 이만큼만 DB에 남기고, 문맥으로도 이만큼만 사용
    private static final int HISTORY_LIMIT = 20;

    // 한 답변에 카드로 보여줄 참조(업체/아이템/답례품) 최대 개수
    private static final int MAX_REFERENCES = 5;

    // 웨딩 관련 질문을 "무관한 질문"으로 과도하게 거절하지 않도록, 그리고 카드가 별도로 뜨니
    // 텍스트 답변은 중복 나열 없이 짧게 요약하도록 명확히 지시하는 프롬프트로 정비
    private static final String SYSTEM_PROMPT =
            "당신은 웨딩 준비 전문 AI 비서입니다. 결혼 준비(웨딩홀, 스튜디오, 드레스, 메이크업, 답례품 등)와 " +
                    "관련된 질문에 답변합니다.\n\n" +
                    "중요: 매 사용자 메시지마다 search_gifts, search_companies, get_company_detail, " +
                    "answer_directly 이 4개 함수 중 정확히 하나를 반드시 호출해야 합니다. 함수를 호출하지 않고 " +
                    "곧바로 텍스트로만 답변하는 것은 허용되지 않습니다. 실제 데이터 조회가 필요 없는 경우에는 " +
                    "answer_directly를 호출하세요.\n\n" +
                    "질문은 아래 기준으로 판단해서 답변하세요.\n\n" +
                    "1) 결혼 준비와 정말 무관한 질문(예: 날씨, 요리 레시피, 스포츠, 코딩 등 웨딩과 전혀 관계없는 화제)에는 " +
                    "answer_directly를 호출한 뒤 '결혼 준비와 관련된 질문만 답변해 드릴 수 있습니다.'라고 정중하게 " +
                    "답하세요. 웨딩홀/드레스/스튜디오/메이크업/답례품/결혼 준비 절차 등과 조금이라도 관련 있으면 절대 " +
                    "이 경우로 처리하지 말고 아래 2), 3)번 기준으로 판단하세요.\n\n" +
                    "2) 결혼 준비와 관련된 일반 지식/조언 질문(예: 결혼식 예절, 청첩장 문구, 결혼을 많이 하는 달/계절, " +
                    "예산 짜는 법, 웨딩 준비 순서, 스몰웨딩 팁 등 우리 서비스의 상품·업체 데이터와 무관한 질문), " +
                    "또는 이전 대화에서 이미 조회한 내용만으로 충분히 답할 수 있는 질문(예: 방금 얘기한 업체의 " +
                    "주소를 다시 묻는 경우)에는 answer_directly를 호출한 뒤, 당신이 알고 있는 지식이나 이전 대화 " +
                    "내용을 바탕으로 직접 친절하고 자연스럽게 답변하세요. 이런 질문을 거절하거나 얼버무리지 마세요. " +
                    "다만 특정 업체/상품의 최신 정보나 아직 조회하지 않은 세부 정보(가격, 옵션 등)가 필요하면 " +
                    "이 경우가 아니라 아래 3)번으로 처리하세요.\n\n" +
                    "3) 우리 사이트에 등록된 업체/상품 데이터를 새로 조회해야 답할 수 있는 질문 - 아래 함수들 중 " +
                    "정확히 맞는 것을 사용하세요.\n" +
                    "   - 답례품/하객 선물(그릇, 수건, 디퓨저, 차, 과자, 비누 등)을 찾거나 추천받고 싶어하면 " +
                    "search_gifts 함수를 사용하세요.\n" +
                    "   - 웨딩드레스(대여/구매/드레스샵 포함), 웨딩홀, 스튜디오, 메이크업 업체를 찾거나 추천받고 싶어하면 " +
                    "search_companies 함수를 사용하세요. 드레스는 상품이 아니라 category=DRESS인 업체(드레스샵)로 " +
                    "등록되어 있으므로, 드레스 관련 질문에는 절대 search_gifts를 쓰지 말고 반드시 search_companies를 " +
                    "사용하세요.\n" +
                    "   - 사용자가 특정 업체 하나를 콕 집어서 그 업체의 세부 상품/가격/옵션(드레스 아이템 목록, " +
                    "홀 대관 옵션, 메이크업 패키지 등)을 알고 싶어하면 get_company_detail 함수를 사용하세요. " +
                    "cmno(업체번호)를 모르면 먼저 search_companies로 그 업체를 찾아서 결과에 있는 업체번호를 확인한 뒤 " +
                    "get_company_detail을 호출하세요. 스튜디오는 세부 상품 목록이 없고 테마 태그 정보만 있습니다.\n" +
                    "   - '인기있는', '괜찮은', '추천할만한', '가성비 좋은'처럼 정확한 필터 조건이 없는 모호한 " +
                    "질문이어도 절대 거절하지 말고, 일단 필터 없이(또는 파악되는 조건만으로) 함수를 호출해서 실제 " +
                    "데이터를 가져온 뒤 그 안에서 최선을 다해 추천하세요. 인기도처럼 우리 데이터에 없는 기준을 " +
                    "물으면, 그 기준의 데이터는 없다고 짧게 언급하되 답변 자체는 거절하지 말고 실제 등록된 업체/상품 " +
                    "중 몇 곳을 추천하세요.\n\n" +
                    "화면 표시 안내: 함수 호출 결과는 이름/가격/이미지가 담긴 카드로 화면에 자동으로 함께 표시됩니다. " +
                    "따라서 답변 텍스트에서는 조회된 업체·상품의 이름, 가격, 설명을 다시 나열하지 말고, 질문에 대한 " +
                    "짧은 요약과 추천 이유만 2~3문장 이내로 간결하게 답하세요. 자세한 목록은 카드를 통해 확인할 수 " +
                    "있습니다. 실제 등록된 데이터에 없는 내용은 절대 지어내지 마세요.\n\n" +
                    "개수 지정: 사용자가 '3개만', '두 곳만'처럼 원하는 개수를 말하면 search_gifts/search_companies의 " +
                    "limit 파라미터에 그 숫자를 그대로 전달하세요. 개수를 말하지 않았으면 limit을 생략하세요. " +
                    "카드는 항상 이 limit(또는 기본값)만큼만 표시되니, 텍스트에서 언급하는 개수와 반드시 일치시키세요.\n\n" +
                    "대화 기록 안내: 이전 대화의 assistant 메시지 끝에 '(참고용 메타데이터...)' 형태의 문구가 붙어 " +
                    "있을 수 있습니다. 이는 사용자에게 보여진 적 없는, 당신만을 위한 내부 참고용 정보(직전에 조회했던 " +
                    "업체/상품의 이름과 id)입니다. 사용자가 '그 중 한 곳', '아까 그 업체'처럼 이전에 조회된 대상을 " +
                    "다시 언급하면 이 메타데이터에서 해당 항목의 id를 찾아 cmno로 사용해 get_company_detail을 " +
                    "호출하세요(이런 경우는 2)번이 아니라 3)번으로 처리 - answer_directly를 호출하면 안 됩니다). " +
                    "이 메타데이터 형식을 당신의 답변에 그대로 출력하지 마세요.\n\n" +
                    "출력 형식: 답변이 표시되는 화면은 마크다운을 렌더링하지 않는 일반 텍스트 채팅창입니다. " +
                    "**볼드**, # 제목, `코드블록`, [링크](url), 표 같은 마크다운 문법을 절대 사용하지 마세요. " +
                    "강조하고 싶은 내용은 문장으로 자연스럽게 풀어 쓰고, 목록이 필요하면 '- '로 시작하는 줄바꿈 " +
                    "목록만 사용하세요.";

    // 함수 실행 결과 - AI에게 줄 텍스트와, 프론트에 카드로 줄 참조 데이터를 같이 담음
    private record ToolResult(String text, List<ChatReferenceDTO> references) {
        private static ToolResult textOnly(String text) {
            return new ToolResult(text, List.of());
        }
    }

    // DB save/delete가 saveAndTrim 안에서 이어지는데, deleteOlderThan은 @Modifying 쿼리라
    // 트랜잭션 없이 호출되면 "Executing an update/delete query" 에러가 남 - 여기서 감싸줌
    @Transactional
    public ChatResponseDTO getAnswer(String memberEmail, String question) {

        // 1) 회원의 최근 대화(최대 HISTORY_LIMIT개, 최신순)를 불러와서 문맥으로 사용할 메시지 목록 구성
        List<ChatMessage> history = chatMessageRepository
                .findRecentByMember(memberEmail, PageRequest.of(0, HISTORY_LIMIT));

        List<OpenAiMessageDTO> messages = new ArrayList<>();
        messages.add(OpenAiMessageDTO.of("system", SYSTEM_PROMPT));

        // findRecentByMember는 최신순(cno desc)으로 오기 때문에, 대화 순서로 쓰려면 뒤집어야 함
        for (int i = history.size() - 1; i >= 0; i--) {
            ChatMessage m = history.get(i);
            messages.add(OpenAiMessageDTO.of(m.getRole(), m.getContent()));
        }

        messages.add(OpenAiMessageDTO.of("user", question));

        // 2) 1차 호출 - 답례품 검색 + 업체 검색 + 업체 상세 + "조회 불필요" 함수를 tools로 같이 전달.
        // tool_choice="required"로 강제해서, 모델이 아무 함수도 안 부르고 바로 텍스트로 답해버리는(그래서
        // 카드가 안 뜨는) 경우를 막는다 - 조회가 필요 없을 땐 answer_directly를 고르게 유도
        OpenAiResponseDTO firstResponse = openAiClient.getChatCompletions(
                messages,
                List.of(searchGiftsTool(), searchCompaniesTool(), getCompanyDetailTool(), answerDirectlyTool()),
                "required");

        OpenAiMessageDTO assistantMessage = firstResponse.getChoices().getFirst().getMessage();

        String finalAnswer;
        List<ChatReferenceDTO> references = new ArrayList<>();

        if (assistantMessage.getToolCalls() != null && !assistantMessage.getToolCalls().isEmpty()) {

            // 3) 모델이 함수 호출을 요청한 경우: 실제로 함수를 실행하고, 그 결과를 들고 2차 호출
            List<OpenAiMessageDTO> followUp = new ArrayList<>(messages);
            followUp.add(assistantMessage); // "이 함수 호출할게"라고 한 메시지 그대로 포함시켜야 함

            for (OpenAiMessageDTO.ToolCallDTO toolCall : assistantMessage.getToolCalls()) {

                ToolResult result = executeToolCall(toolCall);
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

    // 함수가 3개가 됐으니 이름으로 분기
    private ToolResult executeToolCall(OpenAiMessageDTO.ToolCallDTO toolCall) {

        String functionName = toolCall.getFunction().getName();

        return switch (functionName) {
            case "search_gifts" -> executeSearchGifts(toolCall);
            case "search_companies" -> executeSearchCompanies(toolCall);
            case "get_company_detail" -> executeGetCompanyDetail(toolCall);
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

            List<String> categories = category != null ? List.of(category) : null;

            // 사용자가 개수를 지정했으면 그 개수를, 아니면 기본 5개까지만 - 텍스트/카드 개수를 일치시키기 위해
            // 여기서 페이지 크기 자체를 제한해서 이후 텍스트/카드가 동일한 목록을 그대로 쓰게 한다
            int effectiveLimit = (limit != null && limit > 0) ? Math.min(limit, MAX_REFERENCES) : MAX_REFERENCES;

            var page = productRepository.searchProductList(
                    categories, keyword, minPrice, maxPrice, minRating,
                    PageRequest.of(0, effectiveLimit, Sort.by("ratingAvg").descending())
            );

            List<Product> products = page.getContent().stream()
                    .map(row -> (Product) row[0])
                    .collect(Collectors.toList());

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
    private ToolResult executeSearchCompanies(OpenAiMessageDTO.ToolCallDTO toolCall) {

        try {
            JsonNode args = objectMapper.readTree(toolCall.getFunction().getArguments());

            CompanyCategory category = null;
            if (args.hasNonNull("category")) {
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

            // 사용자가 개수를 지정했으면 그 개수를, 아니면 기본 5개까지만
            int effectiveLimit = (limit != null && limit > 0) ? Math.min(limit, MAX_REFERENCES) : MAX_REFERENCES;

            // DB에서는 후보 15개까지 넉넉히 가져오되, 실제 텍스트/카드로 내보내는 건 effectiveLimit개로 맞춰서
            // 텍스트에서 설명하는 개수와 카드 개수가 항상 일치하도록 한다
            var page = companyRepository.searchList(
                    category, keyword, minPrice, maxPrice, PageRequest.of(0, 15));

            List<Company> companies = page.getContent().stream()
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
                        detail.getItems().forEach(item -> sb.append(String.format(
                                "- %s (%,d원, 타입: %s, 스타일: %s, 사이즈: %s)\n",
                                item.getItemName(),
                                item.getPrice() != null ? item.getPrice().longValue() : 0L,
                                item.getItemType(),
                                item.getStyleTags() != null ? item.getStyleTags() : "정보없음",
                                item.getSizeRange() != null ? item.getSizeRange() : "정보없음")));

                        detail.getItems().stream().limit(MAX_REFERENCES).forEach(item ->
                                refs.add(ChatReferenceDTO.builder()
                                        .type("DRESS_ITEM")
                                        .id(item.getDressItemId())
                                        .name(item.getItemName())
                                        .imageUrl(itemImagePath(item.getImageUrl()))
                                        .priceLabel(item.getPrice() != null
                                                ? String.format("%,d원", item.getPrice().longValue())
                                                : "가격 정보없음")
                                        .link("/dress-items/read/" + item.getDressItemId())
                                        .build()));
                    }
                }
                case HALL -> {
                    HallDetailDTO detail = company.getHallDetail();
                    if (detail == null || detail.getItems() == null || detail.getItems().isEmpty()) {
                        sb.append("등록된 홀 대관 옵션이 없습니다.");
                    } else {
                        detail.getItems().forEach(item -> sb.append(String.format(
                                "- %s (%,d원, 수용인원: %s명, 식사타입: %s)\n",
                                item.getItemName(),
                                item.getPrice() != null ? item.getPrice().longValue() : 0L,
                                item.getCapacity() != null ? item.getCapacity() : "정보없음",
                                item.getMealType())));

                        // 홀 옵션은 별도 상세페이지가 없어서 업체 페이지로 이동시킴
                        detail.getItems().stream().limit(MAX_REFERENCES).forEach(item ->
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
                case MAKEUP -> {
                    MakeupDetailDTO detail = company.getMakeupDetail();
                    if (detail == null || detail.getPackages() == null || detail.getPackages().isEmpty()) {
                        sb.append("등록된 메이크업 패키지가 없습니다.");
                    } else {
                        detail.getPackages().forEach(pkg -> sb.append(String.format(
                                "- %s 패키지 (할인율 %s%%)\n",
                                pkg.getPackageType(),
                                pkg.getDiscountRate() != null ? pkg.getDiscountRate().toPlainString() : "0")));

                        // 메이크업 패키지도 별도 상세페이지가 없어서 업체 페이지로 이동시킴
                        detail.getPackages().stream().limit(MAX_REFERENCES).forEach(pkg ->
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

    // search_gifts 함수 스펙 - 답례품 전용. category enum은 2026-07-15 기준 실제 DB에 등록된 값
    // (곡물/식품, 과자/한과, 디퓨저/향수, 비누/핸드워시, 생활/건강, 식기/머그, 차/커피, 타월).
    // 새 카테고리가 추가되면 이 목록도 같이 갱신해야 함.
    private OpenAiToolDTO searchGiftsTool() {

        Map<String, Object> properties = Map.of(
                "category", Map.of(
                        "type", "string",
                        "description", "답례품 카테고리",
                        "enum", List.of(
                                "곡물/식품", "과자/한과", "디퓨저/향수", "비누/핸드워시",
                                "생활/건강", "식기/머그", "차/커피", "타월")),
                "keyword", Map.of("type", "string", "description", "상품명/설명에서 찾을 키워드"),
                "minPrice", Map.of("type", "integer", "description", "최소 가격(원)"),
                "maxPrice", Map.of("type", "integer", "description", "최대 가격(원)"),
                "minRating", Map.of("type", "number", "description", "최소 평점 (0~5)"),
                "limit", Map.of("type", "integer", "description", "사용자가 요청한 결과 개수 (예: '3개만'이면 3). 지정 안 하면 기본 5개")
        );

        Map<String, Object> parameters = Map.of(
                "type", "object",
                "properties", properties,
                "required", List.of()
        );

        return OpenAiToolDTO.builder()
                .function(OpenAiToolDTO.FunctionSpec.builder()
                        .name("search_gifts")
                        .description("결혼식 답례품(하객 선물)을 검색합니다. 드레스는 여기서 찾지 말고 " +
                                "search_companies(category=DRESS)를 사용하세요.")
                        .parameters(parameters)
                        .build())
                .build();
    }

    // search_companies 함수 스펙 - CompanyRepository.searchList가 받는 파라미터 그대로 노출
    private OpenAiToolDTO searchCompaniesTool() {

        Map<String, Object> properties = Map.of(
                "category", Map.of(
                        "type", "string",
                        "description", "업체 카테고리",
                        "enum", List.of("HALL", "DRESS", "STUDIO", "MAKEUP")),
                "keyword", Map.of("type", "string", "description", "업체명/주소에서 찾을 키워드 (예: 지역명)"),
                "minPrice", Map.of("type", "number", "description", "최소 평균가격(원)"),
                "maxPrice", Map.of("type", "number", "description", "최대 평균가격(원)"),
                "limit", Map.of("type", "integer", "description", "사용자가 요청한 결과 개수 (예: '3개만', '두 곳만'이면 3, 2). 지정 안 하면 기본 5개")
        );

        Map<String, Object> parameters = Map.of(
                "type", "object",
                "properties", properties,
                "required", List.of()
        );

        return OpenAiToolDTO.builder()
                .function(OpenAiToolDTO.FunctionSpec.builder()
                        .name("search_companies")
                        .description("웨딩홀/드레스업체/스튜디오/메이크업 업체를 조건에 맞게 검색합니다. " +
                                "설명(description)과 업체번호(cmno)까지 함께 반환되니, 분위기/느낌에 맞는 곳을 판단해서 추천하고 " +
                                "세부 상품이 궁금하면 그 업체번호로 get_company_detail을 호출하세요. 정확한 필터 조건이 없어도 " +
                                "일단 호출해서 실제 데이터를 확인한 뒤 추천하세요.")
                        .parameters(parameters)
                        .build())
                .build();
    }

    // get_company_detail 함수 스펙 - 업체 하나의 세부 상품/옵션(드레스 아이템, 홀 대관 옵션, 메이크업 패키지) 조회
    private OpenAiToolDTO getCompanyDetailTool() {

        Map<String, Object> properties = Map.of(
                "cmno", Map.of("type", "integer", "description", "조회할 업체의 업체번호 (search_companies 결과에서 확인)")
        );

        Map<String, Object> parameters = Map.of(
                "type", "object",
                "properties", properties,
                "required", List.of("cmno")
        );

        return OpenAiToolDTO.builder()
                .function(OpenAiToolDTO.FunctionSpec.builder()
                        .name("get_company_detail")
                        .description("업체번호(cmno)로 특정 업체 하나의 세부 상품/옵션을 조회합니다. " +
                                "드레스업체면 드레스 아이템 목록, 웨딩홀이면 대관 옵션 목록, 메이크업이면 패키지 목록을 반환합니다. " +
                                "스튜디오는 세부 상품이 없고 테마 태그만 반환됩니다.")
                        .parameters(parameters)
                        .build())
                .build();
    }

    // answer_directly 함수 스펙 - 실제 DB 조회 없이 답할 수 있는 턴에 tool_choice="required"를 만족시키기 위한 더미 함수
    private OpenAiToolDTO answerDirectlyTool() {

        Map<String, Object> parameters = Map.of(
                "type", "object",
                "properties", Map.of(),
                "required", List.of()
        );

        return OpenAiToolDTO.builder()
                .function(OpenAiToolDTO.FunctionSpec.builder()
                        .name("answer_directly")
                        .description("실제 업체/상품 데이터 조회가 필요 없을 때 호출하세요. 결혼 준비와 무관한 질문, " +
                                "우리 데이터 없이 답할 수 있는 일반 지식/조언 질문, 또는 이전 대화에서 이미 조회한 " +
                                "내용만으로 충분히 답할 수 있는 질문에 사용합니다. 이 함수는 아무 데이터도 반환하지 " +
                                "않으며, 호출 후 당신이 직접 답변을 작성하면 됩니다.")
                        .parameters(parameters)
                        .build())
                .build();
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