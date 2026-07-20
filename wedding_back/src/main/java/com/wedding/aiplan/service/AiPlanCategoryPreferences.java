package com.wedding.aiplan.service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.wedding.aiplan.dto.AiPlanDetailRequestDTO;
import com.wedding.company.domain.HallType;
import com.wedding.company.domain.MakeupPackageType;

// 자세히 모드에서 입력한 카테고리별 취향. 빠르게 모드는 전부 null인 empty()를 씀.
// 홀/스튜디오/드레스는 칩 중복선택이 가능해서 리스트로 받는다 - 홀은 업체 하나에 타입이 하나뿐이라
// "선택한 타입들 중 하나라도 맞으면"(OR), 스튜디오/드레스는 태그가 여러 개 같이 붙어있는 값이라
// "선택한 태그를 전부 가지고 있어야"(AND) 매칭한다 (AiPlanCandidateBuilder 참고).
// 메이크업은 MakeupPackageType(HAIR/MAKEUP/NAIL/HAIR_MAKEUP/HAIR_NAIL/MAKEUP_NAIL/FULL) 구조화 값으로
// 단일 선택만 매칭한다 - 예전엔 구조화 필드가 없다고 보고 Company.description 키워드 매칭을 썼었는데,
// MakeupPackage/MakeupPackageType이 실제로 있는 걸 확인해서 다른 카테고리처럼 구조화 매칭으로 바꿈.
public class AiPlanCategoryPreferences {

    private final List<HallType> hallTypes;
    private final List<String> studioKeywords;
    private final List<String> dressKeywords;
    private final MakeupPackageType makeupType;

    private AiPlanCategoryPreferences(List<HallType> hallTypes, List<String> studioKeywords,
                                      List<String> dressKeywords, MakeupPackageType makeupType) {
        this.hallTypes = hallTypes;
        this.studioKeywords = studioKeywords;
        this.dressKeywords = dressKeywords;
        this.makeupType = makeupType;
    }

    public static AiPlanCategoryPreferences of(List<HallType> hallTypes, List<String> studioKeywords,
                                               List<String> dressKeywords, MakeupPackageType makeupType) {
        return new AiPlanCategoryPreferences(hallTypes, studioKeywords, dressKeywords, makeupType);
    }

    public static AiPlanCategoryPreferences empty() {
        return new AiPlanCategoryPreferences(List.of(), List.of(), List.of(), null);
    }

    // AiPlanDetailRequestDTO -> preferences 변환 (AiPlanDetailServiceImpl / AiPlanAiServiceImpl 공용)
    // 프론트는 칩 중복선택 결과를 콤마로 이어 붙인 문자열 하나로 보낸다 (예: "GARDEN,BANQUET").
    public static AiPlanCategoryPreferences fromDetailRequest(AiPlanDetailRequestDTO dto) {
        return of(
                parseHallTypes(dto.getHallType()),
                splitToKeywords(dto.getStudioMood()),
                splitToKeywords(dto.getDressStyle()),
                parseMakeupType(dto.getMakeupStyle()));
    }

    // 프론트에서 잘못된 값을 보내도 500 대신 그 값만 조용히 무시
    private static List<HallType> parseHallTypes(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Stream.of(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(AiPlanCategoryPreferences::parseHallType)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private static HallType parseHallType(String raw) {
        try {
            return HallType.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static List<String> splitToKeywords(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Stream.of(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    private static MakeupPackageType parseMakeupType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return MakeupPackageType.fromString(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public boolean isEmpty() {
        return hallTypes.isEmpty() && studioKeywords.isEmpty() && dressKeywords.isEmpty() && makeupType == null;
    }

    public List<HallType> getHallTypes() {
        return hallTypes;
    }

    public List<String> getStudioKeywords() {
        return studioKeywords;
    }

    public List<String> getDressKeywords() {
        return dressKeywords;
    }

    public MakeupPackageType getMakeupType() {
        return makeupType;
    }
}
