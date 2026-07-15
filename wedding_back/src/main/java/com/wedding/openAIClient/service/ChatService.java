package com.wedding.openAIClient.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedding.company.domain.Company;
import com.wedding.company.domain.CompanyCategory;
import com.wedding.company.repository.CompanyRepository;
import com.wedding.global.util.OpenAiClient;
import com.wedding.openAIClient.controller.ChatMessageRepository;
import com.wedding.openAIClient.domain.ChatMessage;
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
    private final ObjectMapper objectMapper;

    // 회원별로 대화를 이만큼만 DB에 남기고, 문맥으로도 이만큼만 사용
    private static final int HISTORY_LIMIT = 20;

    private static final String SYSTEM_PROMPT =
            "당신은 웨딩 준비 전문 AI 비서입니다. 결혼 준비와 관련된 질문에만 답변합니다.\n\n" +
                    "질문은 아래 세 가지 중 하나로 판단해서 답변하세요.\n\n" +
                    "1) 결혼과 전혀 관련 없는 질문(예: 음식 추천, 날씨, 다른 주제의 일상 대화)\n" +
                    "   -> 반드시 '결혼 준비와 관련된 질문만 답변해 드릴 수 있습니다.'라고만 정중하게 답하고, 다른 내용은 덧붙이지 마세요.\n\n" +
                    "2) 결혼 준비와 관련된 일반 지식/조언 질문(예: 결혼식 예절, 청첩장 문구, 결혼을 많이 하는 달/계절, " +
                    "예산 짜는 법, 웨딩 준비 순서, 스몰웨딩 팁 등 우리 서비스의 상품·업체 데이터와 무관한 질문)\n" +
                    "   -> 함수를 호출하지 말고, 당신이 알고 있는 일반 지식으로 직접 친절하고 자연스럽게 답변하세요. " +
                    "이런 질문을 거절하거나 얼버무리지 마세요.\n\n" +
                    "3) 우리 사이트에 등록된 드레스 상품이나 웨딩 업체(홀/드레스업체/스튜디오/메이크업)를 찾거나 추천받고 싶어하는 질문\n" +
                    "   -> 드레스 상품이면 search_dresses 함수를, 웨딩홀/드레스업체/스튜디오/메이크업 업체(느낌, 분위기, 가격, 지역 포함)면 " +
                    "search_companies 함수를 사용해서 실제 등록된 데이터 중에서만 답변하세요. 데이터에 없는 내용을 지어내지 마세요.";

    // 수정시작
    // DB save/delete가 saveAndTrim 안에서 이어지는데, deleteOlderThan은 @Modifying 쿼리라
    // 트랜잭션 없이 호출되면 "Executing an update/delete query" 에러가 남 - 여기서 감싸줌
    @Transactional
    public String getAnswer(String memberEmail, String question) {
        // 수정끝

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

        // 2) 1차 호출 - 상품 검색 + 업체 검색 함수를 tools로 같이 전달
        OpenAiResponseDTO firstResponse = openAiClient.getChatCompletions(
                messages, List.of(searchDressesTool(), searchCompaniesTool()));

        OpenAiMessageDTO assistantMessage = firstResponse.getChoices().getFirst().getMessage();

        String finalAnswer;

        if (assistantMessage.getToolCalls() != null && !assistantMessage.getToolCalls().isEmpty()) {

            // 3) 모델이 함수 호출을 요청한 경우: 실제로 함수를 실행하고, 그 결과를 들고 2차 호출
            List<OpenAiMessageDTO> followUp = new ArrayList<>(messages);
            followUp.add(assistantMessage); // "이 함수 호출할게"라고 한 메시지 그대로 포함시켜야 함

            for (OpenAiMessageDTO.ToolCallDTO toolCall : assistantMessage.getToolCalls()) {

                String resultText = executeToolCall(toolCall);

                followUp.add(OpenAiMessageDTO.builder()
                        .role("tool")
                        .toolCallId(toolCall.getId())
                        .content(resultText)
                        .build());
            }

            OpenAiResponseDTO secondResponse = openAiClient.getChatCompletions(followUp, null);
            finalAnswer = secondResponse.getChoices().getFirst().getMessage().getContent();

        } else {
            // 함수 호출 없이 바로 답변한 경우
            finalAnswer = assistantMessage.getContent();
        }

        // 4) DB에는 실제 질문/최종답변 2건만 저장 (함수호출 중간 메시지는 저장 안 함)
        saveAndTrim(memberEmail, question, finalAnswer);

        return finalAnswer;
    }

    // 함수가 2개가 됐으니 이름으로 분기
    private String executeToolCall(OpenAiMessageDTO.ToolCallDTO toolCall) {

        String functionName = toolCall.getFunction().getName();

        return switch (functionName) {
            case "search_dresses" -> executeSearchProducts(toolCall);
            case "search_companies" -> executeSearchCompanies(toolCall);
            default -> "지원하지 않는 함수입니다: " + functionName;
        };
    }

    // 상품(답례품) 검색 실행
    private String executeSearchProducts(OpenAiMessageDTO.ToolCallDTO toolCall) {

        try {
            JsonNode args = objectMapper.readTree(toolCall.getFunction().getArguments());

            String category = args.hasNonNull("category") ? args.get("category").asText() : null;
            String keyword = args.hasNonNull("keyword") ? args.get("keyword").asText() : null;
            Integer minPrice = args.hasNonNull("minPrice") ? args.get("minPrice").asInt() : null;
            Integer maxPrice = args.hasNonNull("maxPrice") ? args.get("maxPrice").asInt() : null;
            Double minRating = args.hasNonNull("minRating") ? args.get("minRating").asDouble() : null;

            List<String> categories = category != null ? List.of(category) : null;

            // 평점 높은 순 5개까지만 - 프롬프트 길이/비용 고려한 기본값 (필요하면 조정 가능)
            var page = productRepository.searchProductList(
                    categories, keyword, minPrice, maxPrice, minRating,
                    PageRequest.of(0, 5, Sort.by("ratingAvg").descending())
            );

            List<Product> products = page.getContent().stream()
                    .map(row -> (Product) row[0])
                    .collect(Collectors.toList());

            if (products.isEmpty()) {
                return "조건에 맞는 드레스를 찾지 못했습니다.";
            }

            return products.stream()
                    .map(p -> String.format("- %s (%,d원, 카테고리: %s, 평점: %.1f)",
                            p.getPname(), p.getPrice(), p.getCategory(), p.getRatingAvg()))
                    .collect(Collectors.joining("\n"));

        } catch (Exception e) {
            return "상품 검색 중 오류가 발생했습니다.";
        }
    }

    // 업체(홀/드레스업체/스튜디오/메이크업) 검색 실행 - 설명 텍스트를 통째로 넘겨서
    // AI가 느낌/분위기까지 읽고 판단할 수 있게 함
    private String executeSearchCompanies(OpenAiMessageDTO.ToolCallDTO toolCall) {

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

            // 후보 15개까지 - 설명 텍스트를 통째로 넘길 거라 개수는 넉넉하지 않게 제한
            var page = companyRepository.searchList(
                    category, keyword, minPrice, maxPrice, PageRequest.of(0, 15));

            List<Company> companies = page.getContent();

            if (companies.isEmpty()) {
                return "조건에 맞는 업체를 찾지 못했습니다.";
            }

            return companies.stream()
                    .map(c -> String.format(
                            "- %s (%s) | 주소: %s | 평균가격: %s원\n  설명: %s",
                            c.getName(), c.getCategory(), c.getAddress(),
                            c.getPriceAvg() != null ? c.getPriceAvg().toPlainString() : "정보없음",
                            c.getDescription() != null ? c.getDescription() : "설명 없음"))
                    .collect(Collectors.joining("\n"));

        } catch (Exception e) {
            return "업체 검색 중 오류가 발생했습니다.";
        }
    }

    // search_dresses 함수 스펙 - 기존 searchProductList가 이미 받는 파라미터 그대로 노출
    private OpenAiToolDTO searchDressesTool() {

        Map<String, Object> properties = Map.of(
                "category", Map.of("type", "string", "description", "드레스 카테고리 (예: 미니, 머메이드, 벨라인)"),
                "keyword", Map.of("type", "string", "description", "상품명/설명에서 찾을 키워드"),
                "minPrice", Map.of("type", "integer", "description", "최소 가격(원)"),
                "maxPrice", Map.of("type", "integer", "description", "최대 가격(원)"),
                "minRating", Map.of("type", "number", "description", "최소 평점 (0~5)")
        );

        Map<String, Object> parameters = Map.of(
                "type", "object",
                "properties", properties,
                "required", List.of()
        );

        return OpenAiToolDTO.builder()
                .function(OpenAiToolDTO.FunctionSpec.builder()
                        .name("search_dresses")
                        .description("조건에 맞는 웨딩 드레스 상품을 검색합니다.")
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
                "maxPrice", Map.of("type", "number", "description", "최대 평균가격(원)")
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
                                "설명(description)까지 함께 반환되니 분위기/느낌에 맞는 곳을 판단해서 추천하세요.")
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