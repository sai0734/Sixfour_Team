package com.wedding.quote.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedding.company.domain.CompanyCategory;
import com.wedding.global.util.CustomFileUtil;
import com.wedding.global.util.OpenAiClient;
import com.wedding.openAIClient.dto.OpenAiMessageDTO;
import com.wedding.quote.domain.Quote;
import com.wedding.quote.dto.QuoteCompareResultDTO;
import com.wedding.quote.dto.QuoteDTO;
import com.wedding.quote.dto.QuoteItemDTO;
import com.wedding.quote.repository.QuoteRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// AI 견적서 - 업로드 시점엔 사진 하나에서 고정 스키마만 추출하고(우열 판단 없음), 비교
// 시점에는 "같은 카테고리끼리"만 가격/항목/조건 차이점 + 공통점 + 확인질문을 AI가 뽑아준다.
// "그래서 뭐가 더 좋다"는 문구는 두 프롬프트 어디에도 안 넣는다 - 이게 이 기능의 핵심 제약.
@Service
@RequiredArgsConstructor
@Log4j2
public class QuoteServiceImpl implements QuoteService {

    private final QuoteRepository quoteRepository;
    private final CustomFileUtil customFileUtil;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    private static final String EXTRACT_SYSTEM_PROMPT =
            "당신은 웨딩 준비 견적서 분석가입니다. 사진 속 견적서(웨딩홀/스튜디오/드레스/메이크업 중 "
                    + "하나)를 분석해서 아래 규칙과 JSON 스키마에 맞게 응답합니다. 방금 예시로 보여드린 "
                    + "텍스트 견적서 분석과 같은 방식으로, 이번엔 실제 사진을 보고 작업하세요.\n\n"
                    + "[읽는 순서]\n"
                    + "1. 먼저 사진에 보이는 모든 숫자와 항목명을 표의 행/열 위치까지 하나하나 빠짐없이 "
                    + "확인하세요. 표에 여러 열(예: A안/B안, 대인/소인)이 있으면 지금 적으려는 숫자가 "
                    + "어느 열에 속하는지 매번 다시 확인하세요.\n"
                    + "2. 한 항목에 '수량', '단가', '금액'이 각각 다른 열로 나뉘어 있으면, items의 "
                    + "price에는 반드시 '금액'(수량×단가로 계산된 최종 합계) 열의 값을 넣으세요. 단가만 "
                    + "넣으면 안 됩니다.\n"
                    + "3. 콤마(,)로 구분된 큰 숫자는 콤마 뒤에 있는 자릿수를 절대 빠뜨리지 말고 전부 "
                    + "이어서 읽으세요. 예를 들어 '97,078,300'이라는 숫자는 반드시 97078300으로 읽어야 "
                    + "하고, 97078처럼 뒷자리를 잘라서 읽으면 안 됩니다.\n"
                    + "4. 문서 상단 로고나 제목에 웨딩홀/스튜디오 같은 실제 업체명이 명확히 적혀있으면 "
                    + "vendorNameGuess에 채우세요. 단, 사진 구석에 작게 있는 출처 표시·블로그 주소· "
                    + "신문사나 매체 이름·사진 저작권 표시(예: '경향신문', 'blog.naver.com/...')는 "
                    + "그 웨딩홀의 업체명이 아니니 절대 넣지 마세요. 확신이 안 서면 null로 두세요.\n\n"
                    + "[숫자를 모를 때]\n"
                    + "totalPrice, perGuestPrice, items 안의 price 모두에 적용됩니다: 사진에서 그 값을 "
                    + "명확히 찾지 못했거나 확신이 없으면 절대로 0을 쓰지 말고 반드시 JSON null을 "
                    + "쓰세요. 0은 '실제로 0원'이라는 뜻이라 '모르겠다'는 의미로 쓰면 안 됩니다. 다른 "
                    + "항목의 가격을 더하거나 빼서 총액을 임의로 계산해내지 마세요 - 사진에 총액이 "
                    + "명시돼 있을 때만 totalPrice를 채우세요.\n\n"
                    + "이 견적이 다른 견적보다 좋은지 나쁜지 판단하거나 추천하지 마세요 - 오직 사진에 "
                    + "보이는 사실과 확인이 필요한 지점만 적으세요. hiddenNotes는 이 견적서 자체에서 "
                    + "주의 깊게 봐야 할 점(예: '장식비 별도 표기', '옵션 추가 시 금액 변동 가능')만 "
                    + "1~4개, 각 문장은 짧게. category는 반드시 HALL/STUDIO/DRESS/MAKEUP 중 하나로만 "
                    + "판단하고, 확신이 없으면 null로 두세요. 다른 설명, 마크다운, 코드블록 없이 순수 "
                    + "JSON만 출력하세요: {\"category\":\"HALL\" or \"STUDIO\" or \"DRESS\" or "
                    + "\"MAKEUP\" or null, \"vendorNameGuess\":\"...\" or null, "
                    + "\"totalPrice\":숫자 or null, \"perGuestPrice\":숫자 or null, "
                    + "\"items\":[{\"name\":\"...\",\"price\":숫자 or null,\"includedInTotal\":true or false or null}], "
                    + "\"hiddenNotes\":[\"...\"]}";

    // 실제 사진 없이 텍스트로만 된 견적서 예시 - "단가 아니라 금액 넣기", "큰 숫자 안 잘라먹기",
    // "모르면 0 아니라 null" 세 가지를 실제 정답 예시로 한 번 보여줘서 프롬프트 설명보다 더
    // 확실하게 패턴을 각인시킨다.
    private static final List<OpenAiMessageDTO> EXTRACT_FEW_SHOT_TURNS = List.of(
            OpenAiMessageDTO.of("user",
                    "(예시 - 실제 사진 아님) 다음은 견적서 표 내용을 텍스트로 옮긴 것입니다. 이 내용으로 "
                            + "정확히 추출 연습을 해보세요.\n"
                            + "구분: 웨딩홀\n"
                            + "항목 | 수량 | 단가 | 금액\n"
                            + "뷔페(양식) | 500명 | 120,000원 | 60,000,000원\n"
                            + "샴페인 서비스 | 1식 | 300,000원 | 300,000원\n"
                            + "장식비: 별도 문의\n"
                            + "합계: 60,300,000원\n"
                            + "부가세 10% 별도"),
            OpenAiMessageDTO.of("assistant",
                    "{\"category\":\"HALL\",\"vendorNameGuess\":null,\"totalPrice\":60300000,"
                            + "\"perGuestPrice\":null,\"items\":["
                            + "{\"name\":\"뷔페(양식)\",\"price\":60000000,\"includedInTotal\":true},"
                            + "{\"name\":\"샴페인 서비스\",\"price\":300000,\"includedInTotal\":true}],"
                            + "\"hiddenNotes\":[\"장식비 별도 문의\",\"10% 부가세 별도\"]}")
    );

    private static final String EXTRACT_USER_PROMPT = "이 견적서 사진에서 정보를 추출해줘.";

    private static final String COMPARE_SYSTEM_PROMPT =
            "당신은 웨딩 견적서 비교 도우미입니다. 아래 같은 카테고리의 두 견적서 정보를 보고, 어느 쪽이 "
                    + "더 좋다고 판단하거나 추천하지 마세요 - 오직 객관적으로 확인 가능한 차이점·공통점만 "
                    + "나열하고, 사용자가 업체에 직접 확인해야 할 질문도 뽑아주세요. priceDifference는 두 "
                    + "견적서 모두 총액이 있을 때만 'A가 B보다 OOO원 저렴/비쌈' 형태의 자연스러운 문장으로 "
                    + "쓰고, 한쪽이라도 총액 정보가 없으면 '한쪽 견적서에 총액 정보가 없어 정확한 금액 "
                    + "비교가 어렵습니다'처럼 자연스러운 문장으로 안내하세요(빈 값이나 어색한 조각글은 "
                    + "쓰지 마세요). 반드시 아래 JSON 형식으로만 응답하세요: "
                    + "{\"priceDifference\":\"위 규칙에 따른 문장\", "
                    + "\"onlyInA\":[\"항목명(가격)\"], \"onlyInB\":[\"항목명(가격)\"], "
                    + "\"conditionDifferences\":[\"...\"], "
                    + "\"commonNotes\":[\"두 견적서 모두 해당하는 특징/누락된 정보\"], "
                    + "\"suggestedQuestions\":[\"A업체에 물어볼 것: ...\", \"B업체에 물어볼 것: ...\"]}";

    @Override
    public QuoteDTO uploadAndExtract(String memberEmail, MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 견적서 사진이 없습니다.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !(contentType.equals("image/jpeg") || contentType.equals("image/png"))) {
            throw new IllegalArgumentException("JPG/PNG 이미지만 업로드할 수 있습니다.");
        }

        byte[] imageBytes;
        try {
            imageBytes = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("견적서 사진을 읽지 못했습니다.", e);
        }

        // AI 추출을 파일 저장보다 먼저 한다 - 실패하면 아무것도 저장하지 않아서 고아 파일이 안 남는다.
        String raw = openAiClient.describeImageAsJson(
                imageBytes, contentType, EXTRACT_SYSTEM_PROMPT, EXTRACT_FEW_SHOT_TURNS, EXTRACT_USER_PROMPT);

        CompanyCategory category;
        String vendorNameGuess;
        Long totalPrice;
        Long perGuestPrice;
        List<QuoteItemDTO> items;
        List<String> hiddenNotes;

        try {
            JsonNode root = objectMapper.readTree(raw);

            category = root.hasNonNull("category") ? CompanyCategory.valueOf(root.get("category").asText()) : null;
            vendorNameGuess = root.hasNonNull("vendorNameGuess") ? root.get("vendorNameGuess").asText() : null;
            totalPrice = root.hasNonNull("totalPrice") ? root.get("totalPrice").asLong() : null;
            perGuestPrice = root.hasNonNull("perGuestPrice") ? root.get("perGuestPrice").asLong() : null;
            items = parseItems(root.get("items"));
            hiddenNotes = parseStringList(root.get("hiddenNotes"));
        } catch (Exception e) {
            log.error("Quote 추출 응답 파싱 실패. raw={}", raw, e);
            throw new IllegalStateException("견적서 사진에서 정보를 추출하지 못했습니다. 다른 사진으로 다시 시도해주세요.");
        }

        if (category == null) {
            throw new IllegalStateException("어떤 종류의 견적서인지 확인하지 못했어요. 홀/스튜디오/드레스/메이크업 견적서만 지원해요.");
        }

        List<String> savedNames = customFileUtil.saveFiles(List.of(file));
        String imageFileName = savedNames.get(0);

        Quote quote = Quote.builder()
                .memberEmail(memberEmail)
                .imageFileName(imageFileName)
                .category(category)
                .vendorNameGuess(vendorNameGuess)
                .totalPrice(totalPrice)
                .perGuestPrice(perGuestPrice)
                .itemsJson(writeJson(items))
                .hiddenNotesJson(writeJson(hiddenNotes))
                .build();

        Quote saved = quoteRepository.save(quote);

        return toDto(saved, items, hiddenNotes);
    }

    @Override
    public List<QuoteDTO> listByMember(String memberEmail) {

        return quoteRepository.findByMemberEmailOrderByQuoteIdDesc(memberEmail).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public QuoteCompareResultDTO compare(String memberEmail, List<Long> quoteIds) {

        if (quoteIds == null || quoteIds.size() != 2) {
            throw new IllegalArgumentException("비교하려면 견적서 2개를 골라주세요.");
        }

        Quote a = getOwned(memberEmail, quoteIds.get(0));
        Quote b = getOwned(memberEmail, quoteIds.get(1));

        // 카테고리 강제의 실질적 지점 - 홀 견적서와 드레스 견적서처럼 서로 다른 종류는 비교 자체를 막는다.
        if (a.getCategory() != b.getCategory()) {
            throw new IllegalArgumentException("같은 종류의 견적서끼리만 비교할 수 있어요.");
        }

        QuoteDTO dtoA = toDto(a);
        QuoteDTO dtoB = toDto(b);

        String raw = openAiClient.getJsonChatCompletion(List.of(
                        OpenAiMessageDTO.of("system", COMPARE_SYSTEM_PROMPT),
                        OpenAiMessageDTO.of("user", buildComparePrompt(dtoA, dtoB))))
                .getChoices().get(0).getMessage().getContent();

        String priceDifference = null;
        List<String> onlyInA = List.of();
        List<String> onlyInB = List.of();
        List<String> conditionDifferences = List.of();
        List<String> commonNotes = List.of();
        List<String> suggestedQuestions = List.of();

        try {
            JsonNode root = objectMapper.readTree(raw);
            priceDifference = root.hasNonNull("priceDifference") ? root.get("priceDifference").asText() : null;
            onlyInA = parseStringList(root.get("onlyInA"));
            onlyInB = parseStringList(root.get("onlyInB"));
            conditionDifferences = parseStringList(root.get("conditionDifferences"));
            commonNotes = parseStringList(root.get("commonNotes"));
            suggestedQuestions = parseStringList(root.get("suggestedQuestions"));
        } catch (Exception e) {
            // 비교 문구 생성만 실패한 경우, 두 견적서 원본 정보는 그대로 보여준다(조용히 폴백).
            log.warn("Quote 비교 응답 파싱 실패 - 원본 견적서 정보만 반환. raw={}", raw, e);
        }

        return QuoteCompareResultDTO.builder()
                .quoteA(dtoA)
                .quoteB(dtoB)
                .priceDifference(priceDifference)
                .onlyInA(onlyInA)
                .onlyInB(onlyInB)
                .conditionDifferences(conditionDifferences)
                .commonNotes(commonNotes)
                .suggestedQuestions(suggestedQuestions)
                .build();
    }

    @Override
    public void remove(String memberEmail, Long quoteId) {

        Quote quote = getOwned(memberEmail, quoteId);
        customFileUtil.deleteFiles(List.of(quote.getImageFileName()));
        quoteRepository.delete(quote);
    }

    @Override
    public String getOwnedImageFileName(String memberEmail, Long quoteId) {

        return getOwned(memberEmail, quoteId).getImageFileName();
    }

    private Quote getOwned(String memberEmail, Long quoteId) {

        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new NoSuchElementException("견적서를 찾을 수 없습니다."));

        if (!Objects.equals(quote.getMemberEmail(), memberEmail)) {
            throw new IllegalArgumentException("본인이 업로드한 견적서만 볼 수 있습니다.");
        }

        return quote;
    }

    private String buildComparePrompt(QuoteDTO a, QuoteDTO b) {

        StringBuilder sb = new StringBuilder();
        sb.append("[견적서 A]\n");
        appendQuoteForPrompt(sb, a);
        sb.append("\n[견적서 B]\n");
        appendQuoteForPrompt(sb, b);
        return sb.toString();
    }

    private void appendQuoteForPrompt(StringBuilder sb, QuoteDTO q) {

        sb.append("업체: ").append(q.getVendorNameGuess() != null ? q.getVendorNameGuess() : "미상").append('\n');
        sb.append("총액: ").append(q.getTotalPrice() != null ? q.getTotalPrice() + "원" : "정보 없음").append('\n');
        if (q.getPerGuestPrice() != null) {
            sb.append("인당 단가: ").append(q.getPerGuestPrice()).append("원\n");
        }
        sb.append("항목:\n");
        for (QuoteItemDTO item : q.getItems()) {
            sb.append("  - ").append(item.getName())
                    .append(item.getPrice() != null ? " (" + item.getPrice() + "원)" : "")
                    .append(Boolean.TRUE.equals(item.getIncludedInTotal()) ? " [총액 포함]" : "")
                    .append('\n');
        }
        if (!q.getHiddenNotes().isEmpty()) {
            sb.append("주의할 점: ").append(String.join(" / ", q.getHiddenNotes())).append('\n');
        }
    }

    private List<QuoteItemDTO> parseItems(JsonNode itemsNode) {

        List<QuoteItemDTO> result = new ArrayList<>();
        if (itemsNode == null || !itemsNode.isArray()) {
            return result;
        }
        for (JsonNode item : itemsNode) {
            result.add(QuoteItemDTO.builder()
                    .name(item.hasNonNull("name") ? item.get("name").asText() : "항목")
                    .price(item.hasNonNull("price") ? item.get("price").asLong() : null)
                    .includedInTotal(item.hasNonNull("includedInTotal") ? item.get("includedInTotal").asBoolean() : null)
                    .build());
        }
        return result;
    }

    private List<String> parseStringList(JsonNode arrNode) {

        List<String> result = new ArrayList<>();
        if (arrNode == null || !arrNode.isArray()) {
            return result;
        }
        for (JsonNode n : arrNode) {
            if (n != null && !n.isNull()) {
                result.add(n.asText());
            }
        }
        return result;
    }

    private String writeJson(Object obj) {

        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private List<QuoteItemDTO> readItems(String json) {

        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<QuoteItemDTO>>() {
            });
        } catch (Exception e) {
            log.warn("itemsJson 파싱 실패, 빈 목록으로 대체. json={}", json, e);
            return List.of();
        }
    }

    private List<String> readStringList(String json) {

        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            log.warn("문자열 목록 파싱 실패, 빈 목록으로 대체. json={}", json, e);
            return List.of();
        }
    }

    private QuoteDTO toDto(Quote quote) {

        return toDto(quote, readItems(quote.getItemsJson()), readStringList(quote.getHiddenNotesJson()));
    }

    private QuoteDTO toDto(Quote quote, List<QuoteItemDTO> items, List<String> hiddenNotes) {

        return QuoteDTO.builder()
                .quoteId(quote.getQuoteId())
                .category(quote.getCategory())
                .vendorNameGuess(quote.getVendorNameGuess())
                .totalPrice(quote.getTotalPrice())
                .perGuestPrice(quote.getPerGuestPrice())
                .items(items)
                .hiddenNotes(hiddenNotes)
                .regDate(quote.getRegDate())
                .build();
    }
}
