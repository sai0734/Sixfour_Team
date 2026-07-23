package com.wedding.quote.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
import com.wedding.global.util.GoogleVisionOcrClient;
import com.wedding.global.util.OpenAiClient;
import com.wedding.openAIClient.dto.OpenAiMessageDTO;
import com.wedding.quote.domain.Quote;
import com.wedding.quote.domain.QuoteComparison;
import com.wedding.quote.dto.QuoteCompareResultDTO;
import com.wedding.quote.dto.QuoteDTO;
import com.wedding.quote.dto.QuoteItemDTO;
import com.wedding.quote.repository.QuoteComparisonRepository;
import com.wedding.quote.repository.QuoteRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// AI 견적서 - 업로드 시점엔 사진 하나에서 고정 스키마만 추출하고(우열 판단 없음), 비교
// 시점에는 "같은 카테고리끼리"만 가격/항목/조건 차이점 + 공통점 + 확인질문을 AI가 뽑아준다.
// "그래서 뭐가 더 좋다"는 문구는 두 프롬프트 어디에도 안 넣는다 - 이게 이 기능의 핵심 제약.
// 비교할 때마다 QuoteComparison으로 자동 기록되고(AI웨딩플랜 히스토리 배지와 같은 사상),
// 원본 Quote가 나중에 지워져도 기록은 스냅샷으로 남는다.
//
// 추출 파이프라인: 사진 -> Google Vision OCR(글자 인식 전담, 전용 OCR이라 정밀도가 높음) ->
// 원문 텍스트 -> OpenAI 텍스트 전용 호출(구조화 전담) -> JSON. 예전엔 사진을 통째로 OpenAI
// 비전 모델에 보냈는데, 빽빽한 표의 숫자를 잘못 읽는 문제가 반복돼서 "글자 인식"과 "구조화
// 이해"를 분리했다 - OpenAI에는 이제 이미지가 아니라 텍스트만 보내서 비용도 더 저렴하다.
//
// 웨딩 무관 견적서 거절: 자동차/가전제품 견적서, 지원 안 하는 언어 문서 등 웨딩 업체 견적서가
// 아닌 걸 올리면 category=null과 함께 rejectReason에 구체적인 사유를 받아서 그대로 예외로
// 던진다(few-shot 예시로 "자동차 견적서 거절" 패턴 하나를 보여줘서 이 판단 기준을 각인시킴 -
// 실제 거절 대상은 자동차에 한정되지 않고 "웨딩 업체 견적서가 아닌 모든 경우"로 넓게 적용됨).
//
// 가격 차이 문구는 AI가 문장으로 생성하게 하면 큰 숫자에 콤마(,) 구분을 빠뜨리는 경우가 있어서
// (이 프로젝트에서 AI의 숫자 처리 정확도 문제가 반복적으로 있었음), AI한테 맡기지 않고 서버에서
// 저장된 totalPrice 값으로 직접 계산 + 포맷팅한다(buildPriceDifference 참고).
@Service
@RequiredArgsConstructor
@Log4j2
public class QuoteServiceImpl implements QuoteService {

    private final QuoteRepository quoteRepository;
    private final QuoteComparisonRepository quoteComparisonRepository;
    private final CustomFileUtil customFileUtil;
    private final OpenAiClient openAiClient;
    private final GoogleVisionOcrClient visionOcrClient;
    private final ObjectMapper objectMapper;

    private static final String EXTRACT_SYSTEM_PROMPT =
            "당신은 웨딩 준비 견적서 분석가입니다. 아래는 OCR(광학문자인식)로 견적서 사진에서 뽑아낸 "
                    + "원문 텍스트입니다. OCR 특성상 줄바꿈이 뒤섞이거나 표의 칸 구분이 흐트러져 보일 수 "
                    + "있으니, 문맥과 숫자 패턴을 보고 원래 표 구조를 추론하면서 읽으세요. 이 텍스트를 "
                    + "분석해서 아래 규칙과 JSON 스키마에 맞게 응답합니다. 방금 예시로 보여드린 OCR "
                    + "텍스트 분석과 같은 방식으로, 이번엔 실제 텍스트를 보고 작업하세요.\n\n"
                    + "[거절 기준 - 다른 무엇보다 먼저 확인]\n"
                    + "다음 중 하나라도 해당하면 category를 반드시 null로 두고, rejectReason에 왜 거절했는지 "
                    + "한국어 한 문장으로 적으세요(그 경우 다른 필드는 전부 null/빈 배열로 두면 됩니다):\n"
                    + "- 웨딩(결혼식) 업체(웨딩홀/스튜디오/드레스/메이크업) 견적서가 명백히 아닌 경우 "
                    + "(예: 자동차, 가전제품, 식당 메뉴판, 무관한 인물·풍경 사진 등)\n"
                    + "- 한국어나 영어가 아닌 다른 언어(예: 우르두어, 아랍어, 태국어, 중국어 등)로 적혀있어 "
                    + "확신 있게 읽을 수 없는 경우\n"
                    + "이 두 경우에 해당하지 않으면 rejectReason은 반드시 null로 두고 아래 순서대로 "
                    + "평소대로 분석을 계속하세요.\n\n"
                    + "[읽는 순서]\n"
                    + "1. 먼저 텍스트에 나오는 모든 숫자와 항목명을 하나하나 빠짐없이 확인하세요. 같은 "
                    + "항목에 여러 숫자(예: A안/B안, 대인/소인)가 붙어 있으면 앞뒤 문맥으로 어느 쪽 "
                    + "숫자인지 판단하세요.\n"
                    + "2. 한 항목에 '수량', '단가', '금액'이 각각 따로 적혀 있으면, items의 price에는 "
                    + "반드시 '금액'(수량×단가로 계산된 최종 합계) 값을 넣으세요. 단가만 넣으면 안 "
                    + "됩니다.\n"
                    + "3. 콤마(,)로 구분된 큰 숫자는 콤마 뒤에 있는 자릿수를 절대 빠뜨리지 말고 전부 "
                    + "이어서 읽으세요. 예를 들어 '97,078,300'이라는 숫자는 반드시 97078300으로 읽어야 "
                    + "하고, 97078처럼 뒷자리를 잘라서 읽으면 안 됩니다. OCR 특성상 숫자 중간에 공백이나 "
                    + "이상한 기호가 섞여 있을 수 있으니 무시하고 숫자만 이어붙이세요.\n"
                    + "4. 텍스트 상단이나 제목 줄에 웨딩홀/스튜디오 같은 실제 업체명이 명확히 적혀있으면 "
                    + "vendorNameGuess에 채우세요. 단, 출처 표시·블로그 주소·신문사나 매체 이름·사진 "
                    + "저작권 표시(예: '경향신문', 'blog.naver.com/...')는 업체명이 아니니 절대 넣지 "
                    + "마세요. 확신이 안 서면 null로 두세요.\n\n"
                    + "[숫자를 모를 때]\n"
                    + "totalPrice, perGuestPrice, items 안의 price 모두에 적용됩니다: 텍스트에서 그 값을 "
                    + "명확히 찾지 못했거나 확신이 없으면 절대로 0을 쓰지 말고 반드시 JSON null을 "
                    + "쓰세요. 0은 '실제로 0원'이라는 뜻이라 '모르겠다'는 의미로 쓰면 안 됩니다. 다른 "
                    + "항목의 가격을 더하거나 빼서 총액을 임의로 계산해내지 마세요 - 텍스트에 총액이 "
                    + "명시돼 있을 때만 totalPrice를 채우세요.\n\n"
                    + "이 견적이 다른 견적보다 좋은지 나쁜지 판단하거나 추천하지 마세요 - 오직 텍스트에 "
                    + "보이는 사실과 확인이 필요한 지점만 적으세요. hiddenNotes는 이 견적서 자체에서 "
                    + "주의 깊게 봐야 할 점(예: '장식비 별도 표기', '옵션 추가 시 금액 변동 가능')만 "
                    + "1~4개, 각 문장은 짧게. category는 반드시 HALL/STUDIO/DRESS/MAKEUP 중 하나로만 "
                    + "판단하고, 확신이 없으면 null로 두세요. 다른 설명, 마크다운, 코드블록 없이 순수 "
                    + "JSON만 출력하세요: {\"rejectReason\":\"...\" or null, "
                    + "\"category\":\"HALL\" or \"STUDIO\" or \"DRESS\" or \"MAKEUP\" or null, "
                    + "\"vendorNameGuess\":\"...\" or null, "
                    + "\"totalPrice\":숫자 or null, \"perGuestPrice\":숫자 or null, "
                    + "\"items\":[{\"name\":\"...\",\"price\":숫자 or null,\"includedInTotal\":true or false or null}], "
                    + "\"hiddenNotes\":[\"...\"]}";

    // OCR로 뽑은 원문처럼 보이는 텍스트 예시 - "단가 아니라 금액 넣기", "큰 숫자 안 잘라먹기",
    // "모르면 0 아니라 null" 세 가지를 정답 예시로 각인시키고, 두 번째 예시(자동차 견적서)로
    // "웨딩 업체 견적서가 아니면 거절" 패턴도 실제 정답 예시로 보여준다.
    private static final List<OpenAiMessageDTO> EXTRACT_FEW_SHOT_TURNS = List.of(
            OpenAiMessageDTO.of("user",
                    "(예시 - 실제 OCR 결과 아님) 다음은 OCR로 추출한 견적서 원문입니다. 줄바꿈이 뒤섞여 "
                            + "있을 수 있습니다. 이 내용으로 정확히 추출 연습을 해보세요.\n"
                            + "구분 웨딩홀\n"
                            + "항목 수량 단가 금액\n"
                            + "뷔페(양식) 500명 120,000원 60,000,000원\n"
                            + "샴페인 서비스 1식 300,000원 300,000원\n"
                            + "장식비 별도 문의\n"
                            + "합계 60,300,000원\n"
                            + "부가세 10% 별도"),
            OpenAiMessageDTO.of("assistant",
                    "{\"rejectReason\":null,\"category\":\"HALL\",\"vendorNameGuess\":null,\"totalPrice\":60300000,"
                            + "\"perGuestPrice\":null,\"items\":["
                            + "{\"name\":\"뷔페(양식)\",\"price\":60000000,\"includedInTotal\":true},"
                            + "{\"name\":\"샴페인 서비스\",\"price\":300000,\"includedInTotal\":true}],"
                            + "\"hiddenNotes\":[\"장식비 별도 문의\",\"10% 부가세 별도\"]}"),
            OpenAiMessageDTO.of("user",
                    "(예시 - 실제 OCR 결과 아님) 다음은 OCR로 추출한 원문입니다. 이 내용으로 정확히 "
                            + "추출 연습을 해보세요.\n"
                            + "견적서\n"
                            + "차종: 그랜저 하이브리드\n"
                            + "판매가격 38,000,000원\n"
                            + "24개월 할부 안내\n"
                            + "담당 딜러: 홍길동"),
            OpenAiMessageDTO.of("assistant",
                    "{\"rejectReason\":\"이 사진은 웨딩 업체 견적서가 아니라 자동차 견적서로 보입니다.\","
                            + "\"category\":null,\"vendorNameGuess\":null,\"totalPrice\":null,"
                            + "\"perGuestPrice\":null,\"items\":[],\"hiddenNotes\":[]}")
    );

    private static final String COMPARE_SYSTEM_PROMPT =
            "당신은 웨딩 견적서 비교 도우미입니다. 아래 같은 카테고리의 두 견적서 정보를 보고, 어느 쪽이 "
                    + "더 좋다고 판단하거나 추천하지 마세요 - 오직 객관적으로 확인 가능한 차이점·공통점만 "
                    + "나열하고, 사용자가 업체에 직접 확인해야 할 질문도 뽑아주세요. 가격 차이 문구는 "
                    + "서버에서 이미 별도로 정확하게 계산하니 당신이 만들 필요는 없습니다. 반드시 아래 "
                    + "JSON 형식으로만 응답하세요: "
                    + "{\"onlyInA\":[\"항목명(가격)\"], \"onlyInB\":[\"항목명(가격)\"], "
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

        // 1) OCR로 이미지에서 원문 텍스트 추출 - 글자 인식은 전용 OCR 엔진이 AI 비전보다 정밀함.
        String ocrText = visionOcrClient.extractText(imageBytes);
        if (ocrText == null || ocrText.isBlank()) {
            throw new IllegalStateException("사진에서 글자를 읽어내지 못했습니다. 더 선명한 사진으로 다시 시도해주세요.");
        }

        // 2) 그 텍스트를 AI한테 구조화시킴 - 이미지 재전송 없이 텍스트만 보내서 더 저렴함.
        // AI 추출을 파일 저장보다 먼저 한다 - 실패하면 아무것도 저장하지 않아서 고아 파일이 안 남는다.
        List<OpenAiMessageDTO> messages = new ArrayList<>();
        messages.add(OpenAiMessageDTO.of("system", EXTRACT_SYSTEM_PROMPT));
        messages.addAll(EXTRACT_FEW_SHOT_TURNS);
        messages.add(OpenAiMessageDTO.of("user",
                "다음은 OCR로 추출한 견적서 원문입니다. 줄바꿈이 뒤섞여 있을 수 있습니다:\n\n" + ocrText));

        String raw = openAiClient.getJsonChatCompletion(messages)
                .getChoices().get(0).getMessage().getContent();

        CompanyCategory category;
        String vendorNameGuess;
        Long totalPrice;
        Long perGuestPrice;
        List<QuoteItemDTO> items;
        List<String> hiddenNotes;
        String rejectReason;

        try {
            JsonNode root = objectMapper.readTree(raw);

            rejectReason = root.hasNonNull("rejectReason") ? root.get("rejectReason").asText() : null;
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

        // AI가 "웨딩 견적서가 아니다 / 못 읽는 언어다"라고 명시적으로 판단한 경우 - 구체적인 이유 그대로 노출
        if (rejectReason != null && !rejectReason.isBlank()) {
            throw new IllegalStateException(rejectReason);
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

        // 가격 차이는 AI 문장이 아니라 저장된 totalPrice로 서버가 직접 계산 - 큰 숫자에 콤마
        // 구분을 빠뜨리는 등 AI의 숫자 포맷팅 실수를 원천적으로 없앤다.
        String priceDifference = buildPriceDifference(dtoA, dtoB);
        List<String> onlyInA = List.of();
        List<String> onlyInB = List.of();
        List<String> conditionDifferences = List.of();
        List<String> commonNotes = List.of();
        List<String> suggestedQuestions = List.of();

        try {
            JsonNode root = objectMapper.readTree(raw);
            onlyInA = parseStringList(root.get("onlyInA"));
            onlyInB = parseStringList(root.get("onlyInB"));
            conditionDifferences = parseStringList(root.get("conditionDifferences"));
            commonNotes = parseStringList(root.get("commonNotes"));
            suggestedQuestions = parseStringList(root.get("suggestedQuestions"));
        } catch (Exception e) {
            // 비교 문구 생성만 실패한 경우, 두 견적서 원본 정보는 그대로 보여준다(조용히 폴백).
            log.warn("Quote 비교 응답 파싱 실패 - 원본 견적서 정보만 반환. raw={}", raw, e);
        }

        // 비교할 때마다 자동으로 기록을 남긴다(AI웨딩플랜 히스토리 배지와 같은 사상) - 나중에 원본
        // Quote가 30일 TTL로 지워져도 기록은 남아야 하니 QuoteDTO 스냅샷을 통째로 저장해둔다.
        QuoteComparison comparison = QuoteComparison.builder()
                .memberEmail(memberEmail)
                .quoteASnapshotJson(writeJson(dtoA))
                .quoteBSnapshotJson(writeJson(dtoB))
                .priceDifference(priceDifference)
                .onlyInAJson(writeJson(onlyInA))
                .onlyInBJson(writeJson(onlyInB))
                .conditionDifferencesJson(writeJson(conditionDifferences))
                .commonNotesJson(writeJson(commonNotes))
                .suggestedQuestionsJson(writeJson(suggestedQuestions))
                .build();

        QuoteComparison saved = quoteComparisonRepository.save(comparison);

        return toComparisonDto(saved);
    }

    @Override
    public List<QuoteCompareResultDTO> listComparisons(String memberEmail) {

        return quoteComparisonRepository.findByMemberEmailOrderByComparisonIdDesc(memberEmail).stream()
                .map(this::toComparisonDto)
                .toList();
    }

    @Override
    public QuoteCompareResultDTO getComparison(String memberEmail, Long comparisonId) {

        QuoteComparison comparison = quoteComparisonRepository.findById(comparisonId)
                .orElseThrow(() -> new NoSuchElementException("비교 기록을 찾을 수 없습니다."));

        if (!Objects.equals(comparison.getMemberEmail(), memberEmail)) {
            throw new IllegalArgumentException("본인의 비교 기록만 볼 수 있습니다.");
        }

        return toComparisonDto(comparison);
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

    // 두 견적서의 totalPrice를 직접 비교해서 콤마가 포함된 자연스러운 한국어 문장을 만든다.
    // 한쪽이라도 totalPrice가 null이면 금액 비교 자체가 불가능하다는 안내 문장으로 대체한다.
    private String buildPriceDifference(QuoteDTO a, QuoteDTO b) {

        if (a.getTotalPrice() == null || b.getTotalPrice() == null) {
            return "한쪽 견적서에 총액 정보가 없어 정확한 금액 비교가 어렵습니다.";
        }

        long diff = a.getTotalPrice() - b.getTotalPrice();
        if (diff == 0) {
            return "두 견적서의 총액이 같습니다.";
        }

        String formatted = String.format(Locale.KOREA, "%,d", Math.abs(diff));
        return diff < 0
                ? "A가 B보다 " + formatted + "원 저렴합니다."
                : "A가 B보다 " + formatted + "원 비쌉니다.";
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

    private QuoteDTO readQuoteDto(String json) {

        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, QuoteDTO.class);
        } catch (Exception e) {
            log.warn("QuoteDTO 스냅샷 파싱 실패. json={}", json, e);
            return null;
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

    private QuoteCompareResultDTO toComparisonDto(QuoteComparison c) {

        return QuoteCompareResultDTO.builder()
                .comparisonId(c.getComparisonId())
                .regDate(c.getRegDate())
                .quoteA(readQuoteDto(c.getQuoteASnapshotJson()))
                .quoteB(readQuoteDto(c.getQuoteBSnapshotJson()))
                .priceDifference(c.getPriceDifference())
                .onlyInA(readStringList(c.getOnlyInAJson()))
                .onlyInB(readStringList(c.getOnlyInBJson()))
                .conditionDifferences(readStringList(c.getConditionDifferencesJson()))
                .commonNotes(readStringList(c.getCommonNotesJson()))
                .suggestedQuestions(readStringList(c.getSuggestedQuestionsJson()))
                .build();
    }
}
