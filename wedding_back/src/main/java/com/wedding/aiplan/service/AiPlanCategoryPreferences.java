package com.wedding.aiplan.service;

import com.wedding.aiplan.dto.AiPlanDetailRequestDTO;
import com.wedding.company.domain.HallType;

// 자세히 모드에서 입력한 카테고리별 취향. 빠르게 모드는 전부 null인 empty()를 씀.
// 메이크업은 구조화된 스타일 필드가 데이터에 없어서 Company.description 키워드 매칭으로 대체함.
public class AiPlanCategoryPreferences {

    private final HallType hallType;
    private final String studioKeyword;
    private final String dressKeyword;
    private final String makeupKeyword;

    private AiPlanCategoryPreferences(HallType hallType, String studioKeyword,
                                      String dressKeyword, String makeupKeyword) {
        this.hallType = hallType;
        this.studioKeyword = studioKeyword;
        this.dressKeyword = dressKeyword;
        this.makeupKeyword = makeupKeyword;
    }

    public static AiPlanCategoryPreferences of(HallType hallType, String studioKeyword,
                                               String dressKeyword, String makeupKeyword) {
        return new AiPlanCategoryPreferences(hallType, studioKeyword, dressKeyword, makeupKeyword);
    }

    public static AiPlanCategoryPreferences empty() {
        return new AiPlanCategoryPreferences(null, null, null, null);
    }

    // AiPlanDetailRequestDTO -> preferences 변환 (AiPlanDetailServiceImpl / AiPlanAiServiceImpl 공용)
    public static AiPlanCategoryPreferences fromDetailRequest(AiPlanDetailRequestDTO dto) {
        return of(
                parseHallType(dto.getHallType()),
                blankToNull(dto.getStudioMood()),
                blankToNull(dto.getDressStyle()),
                blankToNull(dto.getMakeupStyle()));
    }

    // 프론트에서 잘못된 값을 보내도 500 대신 "취향 없음"으로 안전하게 무시
    private static HallType parseHallType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return HallType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    public boolean isEmpty() {
        return hallType == null && studioKeyword == null && dressKeyword == null && makeupKeyword == null;
    }

    public HallType getHallType() {
        return hallType;
    }

    public String getStudioKeyword() {
        return studioKeyword;
    }

    public String getDressKeyword() {
        return dressKeyword;
    }

    public String getMakeupKeyword() {
        return makeupKeyword;
    }
}
