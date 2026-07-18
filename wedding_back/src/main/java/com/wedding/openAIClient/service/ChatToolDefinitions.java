package com.wedding.openAIClient.service;

import com.wedding.openAIClient.dto.OpenAiToolDTO;
import java.util.List;
import java.util.Map;

// AI 챗봇이 호출할 수 있는 함수(OpenAI Function Calling) 스펙 정의 모음
final class ChatToolDefinitions {

    private ChatToolDefinitions() {
    }

    // intent에 따라 이번 턴에 모델에게 후보로 줄 함수 목록을 정한다.
    // intent가 있으면 그 목적에 맞는 함수(들)만 줘서, 전혀 무관한 함수(예: 답례품 검색)까지
    // 후보에 섞여서 오판하는 걸 막는다.
    static List<OpenAiToolDTO> resolveTools(String intent) {
        if (intent == null) {
            return List.of(searchGiftsTool(), searchCompaniesTool(), getCompanyDetailTool(), answerDirectlyTool());
        }
        return switch (intent) {
            case "SEARCH_GIFTS" -> List.of(searchGiftsTool(), answerDirectlyTool());
            case "SEARCH_COMPANIES" -> List.of(searchCompaniesTool(), getCompanyDetailTool(),
                    findAvailableCompaniesTool(), findTopItemsTool(), answerDirectlyTool());
            default -> List.of(searchGiftsTool(), searchCompaniesTool(), getCompanyDetailTool(), answerDirectlyTool());
        };
    }

    // search_gifts 함수 스펙 - 답례품 전용. category enum은 2026-07-15 기준 실제 DB에 등록된 값
    // (곡물/식품, 과자/한과, 디퓨저/향수, 비누/핸드워시, 생활/건강, 식기/머그, 차/커피, 타월).
    // 새 카테고리가 추가되면 이 목록도 같이 갱신해야 함.
    private static OpenAiToolDTO searchGiftsTool() {

        Map<String, Object> properties = Map.of(
                "category", Map.of(
                        "type", "string",
                        "description", "답례품 카테고리",
                        "enum", List.of(
                                "곡물/식품", "과자/한과", "디퓨저/향수", "비누/핸드워시",
                                "생활/건강", "식기/머그", "차/커피", "타월")),
                "keyword", Map.of("type", "string", "description",
                        "상품명/설명에서 찾을 키워드. 상황/용도(예: 돌잔치 선물)처럼 구조화된 필드가 없는 " +
                                "조건은 이 키워드로만 판단합니다. 동의어가 여러 개 있을 수 있으면 쉼표로 " +
                                "구분해서 한 번에 전달하세요 (예: '돌잔치,백일')."),
                "minPrice", Map.of("type", "integer", "description", "최소 가격(원)"),
                "maxPrice", Map.of("type", "integer", "description", "최대 가격(원)"),
                "minRating", Map.of("type", "number", "description", "최소 평점 (0~5)"),
                "limit", Map.of("type", "integer", "description",
                        "사용자가 요청한 결과 개수 (예: '3개만'이면 3). '가장 비싼 답례품'처럼 단 하나만 " +
                                "묻는 질문이면 반드시 1로 지정하세요. 지정 안 하면 기본 5개"),
                "sortBy", Map.of(
                        "type", "string",
                        "description", "정렬 기준. '가장 비싼 답례품', '제일 저렴한 곳'처럼 가격 순위를 " +
                                "묻는 질문에는 PRICE_DESC/PRICE_ASC를, '평점이 가장 낮은/안좋은'처럼 " +
                                "물으면 RATING_ASC를 지정하고 limit도 정확히 맞추세요. 지정하지 않으면 " +
                                "평점 높은 순(RATING_DESC, 기본값)으로 반환합니다.",
                        "enum", List.of("PRICE_DESC", "PRICE_ASC", "RATING_DESC", "RATING_ASC"))
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

    // search_companies 함수 스펙 - CompanyRepository.searchList가 받는 파라미터 + 카테고리별
    // "실제 보유 옵션" 구조화 필터(dressItemType/hallMealType/makeupPackageType)
    private static OpenAiToolDTO searchCompaniesTool() {

        Map<String, Object> properties = Map.of(
                "category", Map.of(
                        "type", "string",
                        "description", "업체 카테고리",
                        "enum", List.of("HALL", "DRESS", "STUDIO", "MAKEUP")),
                "keyword", Map.of("type", "string", "description",
                        "업체명/주소/설명에서 찾을 키워드. 지역명이나 분위기/느낌 같은 서술적 표현에 " +
                                "사용하세요(설명 텍스트까지 검색). 특정 상품/옵션을 실제로 보유했는지 " +
                                "확인하려는 질문이면 이 필드 대신 아래 dressItemType/hallMealType/" +
                                "makeupPackageType을 사용하세요. 동의어가 여러 개 있을 수 있으면 쉼표로 " +
                                "구분해서 한 번에 전달하세요."),
                "minPrice", Map.of("type", "number", "description", "최소 평균가격(원)"),
                "maxPrice", Map.of("type", "number", "description", "최대 평균가격(원)"),
                "limit", Map.of("type", "integer", "description",
                        "사용자가 요청한 결과 개수 (예: '3개만', '두 곳만'이면 3, 2). '가장 비싼 곳'처럼 " +
                                "단 하나만 묻는 질문이면 반드시 1로 지정하세요. 지정 안 하면 기본 5개"),
                "sortBy", Map.of(
                        "type", "string",
                        "description", "정렬 기준. '가장 비싼 곳', '제일 저렴한 홀'처럼 순위/최상위를 묻는 " +
                                "질문에는 반드시 지정하고, limit도 사용자가 원하는 개수(예: '가장 비싼 곳'=1, " +
                                "'저렴한 순 3곳'=3)로 정확히 맞추세요. 지정하지 않으면 정렬 없이 반환합니다.",
                        "enum", List.of("PRICE_DESC", "PRICE_ASC")),
                "dressItemType", Map.of(
                        "type", "string",
                        "description", "category=DRESS일 때, 실제로 이 타입의 드레스 아이템을 등록해둔 업체만 " +
                                "찾습니다 (설명 텍스트가 아니라 실제 상품 데이터 기준이라 더 정확함). 예: 슈트/턱시도 " +
                                "취급 여부를 물으면 SUIT를 사용하세요.",
                        "enum", List.of("DRESS", "SUIT", "ALINE", "BELL", "MERMAID", "MINI", "SLIM")),
                "hallMealType", Map.of(
                        "type", "string",
                        "description", "category=HALL일 때, 실제로 이 식사 타입의 대관 옵션을 등록해둔 업체만 " +
                                "찾습니다 (설명 텍스트가 아니라 실제 옵션 데이터 기준이라 더 정확함).",
                        "enum", List.of("KOREAN", "WESTERN", "BUFFET", "FUSION", "COURSE", "BOTH")),
                "makeupPackageType", Map.of(
                        "type", "string",
                        "description", "category=MAKEUP일 때, 실제로 이 타입의 패키지를 등록해둔 업체만 " +
                                "찾습니다 (설명 텍스트가 아니라 실제 패키지 데이터 기준이라 더 정확함).",
                        "enum", List.of("HAIR", "MAKEUP", "NAIL", "HAIR_MAKEUP", "HAIR_NAIL", "MAKEUP_NAIL", "FULL"))
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
    private static OpenAiToolDTO getCompanyDetailTool() {

        Map<String, Object> properties = Map.of(
                "cmno", Map.of("type", "integer", "description", "조회할 업체의 업체번호 (search_companies 결과에서 확인)"),
                "sortBy", Map.of(
                        "type", "string",
                        "description", "정렬 기준. '이 업체에서 가장 비싼 드레스는?'처럼 그 업체 안에서 " +
                                "상품/옵션 순위를 물으면 PRICE_DESC/PRICE_ASC를 지정하고 limit도 정확히 " +
                                "맞추세요 (드레스/홀 옵션만 해당, 메이크업 패키지는 절대가격이 없어 적용 안 됨).",
                        "enum", List.of("PRICE_DESC", "PRICE_ASC")),
                "limit", Map.of("type", "integer", "description",
                        "보여줄 상품/옵션 개수. 단 하나만 물으면 1로 지정하세요. 지정 안 하면 기본 5개"),
                "dressItemType", Map.of(
                        "type", "string",
                        "description", "이 업체가 드레스업체일 때, '슈트 목록만 보여줘'처럼 특정 타입만 " +
                                "보고 싶어하면 지정하세요.",
                        "enum", List.of("DRESS", "SUIT", "ALINE", "BELL", "MERMAID", "MINI", "SLIM")),
                "hallMealType", Map.of(
                        "type", "string",
                        "description", "이 업체가 웨딩홀일 때, '뷔페 옵션만 보여줘'처럼 특정 식사 타입만 " +
                                "보고 싶어하면 지정하세요.",
                        "enum", List.of("KOREAN", "WESTERN", "BUFFET", "FUSION", "COURSE", "BOTH")),
                "makeupPackageType", Map.of(
                        "type", "string",
                        "description", "이 업체가 메이크업 업체일 때, '헤어네일 패키지만 보여줘'처럼 특정 " +
                                "패키지 타입만 보고 싶어하면 지정하세요.",
                        "enum", List.of("HAIR", "MAKEUP", "NAIL", "HAIR_MAKEUP", "HAIR_NAIL", "MAKEUP_NAIL", "FULL"))
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
                                "스튜디오는 세부 상품이 없고 테마 태그만 반환됩니다. 특정 타입만 보고 싶어하면 " +
                                "dressItemType/hallMealType/makeupPackageType을 지정하세요.")
                        .parameters(parameters)
                        .build())
                .build();
    }

    // find_available_companies 함수 스펙 - 특정 날짜에 예약이 없는(비어있는) 업체를 카테고리/지역/
    // 가격/보유옵션 조건으로 찾는다
    private static OpenAiToolDTO findAvailableCompaniesTool() {

        Map<String, Object> properties = Map.of(
                "category", Map.of(
                        "type", "string",
                        "description", "예약하려는 업체 카테고리",
                        "enum", List.of("HALL", "DRESS", "STUDIO", "MAKEUP")),
                "date", Map.of("type", "string", "description", "예약 희망 날짜 (YYYY-MM-DD 형식으로 변환해서 전달)"),
                "location", Map.of("type", "string", "description", "희망 지역 키워드 (예: 강남, 홍대). 언급 없으면 생략"),
                "minPrice", Map.of("type", "number", "description", "최소 평균가격(원)"),
                "maxPrice", Map.of("type", "number", "description", "최대 평균가격(원)"),
                "sortBy", Map.of(
                        "type", "string",
                        "description", "정렬 기준. '예약 가능한 곳 중 가장 저렴한 곳'처럼 순위를 묻는 " +
                                "질문에는 PRICE_DESC/PRICE_ASC를 지정하고 limit도 정확히 맞추세요.",
                        "enum", List.of("PRICE_DESC", "PRICE_ASC")),
                "dressItemType", Map.of(
                        "type", "string",
                        "description", "category=DRESS일 때, 실제로 이 타입의 드레스 아이템을 등록해둔 업체만 " +
                                "찾습니다 (예: 슈트도 있고 그 날짜에 예약 가능한 곳).",
                        "enum", List.of("DRESS", "SUIT", "ALINE", "BELL", "MERMAID", "MINI", "SLIM")),
                "hallMealType", Map.of(
                        "type", "string",
                        "description", "category=HALL일 때, 실제로 이 식사 타입의 대관 옵션을 등록해둔 업체만 찾습니다.",
                        "enum", List.of("KOREAN", "WESTERN", "BUFFET", "FUSION", "COURSE", "BOTH")),
                "makeupPackageType", Map.of(
                        "type", "string",
                        "description", "category=MAKEUP일 때, 실제로 이 타입의 패키지를 등록해둔 업체만 찾습니다.",
                        "enum", List.of("HAIR", "MAKEUP", "NAIL", "HAIR_MAKEUP", "HAIR_NAIL", "MAKEUP_NAIL", "FULL")),
                "limit", Map.of("type", "integer", "description", "사용자가 요청한 결과 개수. 지정 안 하면 기본 5개")
        );

        Map<String, Object> parameters = Map.of(
                "type", "object",
                "properties", properties,
                "required", List.of("category", "date")
        );

        return OpenAiToolDTO.builder()
                .function(OpenAiToolDTO.FunctionSpec.builder()
                        .name("find_available_companies")
                        .description("특정 날짜에 예약이 없는(예약 가능한) 업체를 카테고리/지역/가격/보유옵션 " +
                                "조건으로 찾습니다.")
                        .parameters(parameters)
                        .build())
                .build();
    }

    // find_top_items 함수 스펙 - tbl_dress_item/tbl_hall_item의 실제 price 기준으로 개별 상품/옵션
    // 순위를 매겨 반환한다. MAKEUP은 절대가격이 없어(할인율만 존재) 지원 대상에서 제외
    private static OpenAiToolDTO findTopItemsTool() {

        Map<String, Object> properties = Map.of(
                "category", Map.of(
                        "type", "string",
                        "description", "어떤 상품/옵션의 가격 순위를 볼지. MAKEUP은 절대가격이 없어 지원하지 " +
                                "않습니다(할인율만 있음) - 메이크업 관련 순위 질문엔 이 함수를 쓰지 말고 " +
                                "지원하지 않는다고 답하세요.",
                        "enum", List.of("HALL", "DRESS")),
                "sortBy", Map.of(
                        "type", "string",
                        "description", "정렬 기준 (필수)",
                        "enum", List.of("PRICE_DESC", "PRICE_ASC")),
                "limit", Map.of("type", "integer", "description",
                        "사용자가 요청한 결과 개수. '가장 비싼 드레스'처럼 단 하나만 물으면 1로 지정하세요. " +
                                "지정 안 하면 기본 1개")
        );

        Map<String, Object> parameters = Map.of(
                "type", "object",
                "properties", properties,
                "required", List.of("category", "sortBy")
        );

        return OpenAiToolDTO.builder()
                .function(OpenAiToolDTO.FunctionSpec.builder()
                        .name("find_top_items")
                        .description("업체 평균가격이 아니라 개별 드레스 상품/홀 대관 옵션의 실제 등록 가격을 " +
                                "기준으로 순위를 매겨 찾습니다. '가장 비싼 드레스를 보유한 곳', '제일 저렴한 " +
                                "홀 옵션'처럼 상품/옵션 단위 가격을 묻는 질문에 사용하세요. 업체 자체의 " +
                                "평균가격 순위(예: '가장 비싼 웨딩홀 업체')를 물으면 이 함수 대신 " +
                                "search_companies의 sortBy를 사용하세요.")
                        .parameters(parameters)
                        .build())
                .build();
    }

    // answer_directly 함수 스펙 - 실제 DB 조회 없이 답할 수 있는 턴에 tool_choice="required"를 만족시키기 위한 더미 함수
    private static OpenAiToolDTO answerDirectlyTool() {

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
}
