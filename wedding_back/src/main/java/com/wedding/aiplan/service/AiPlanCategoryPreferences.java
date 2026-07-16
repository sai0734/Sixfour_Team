package com.wedding.aiplan.service;

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
