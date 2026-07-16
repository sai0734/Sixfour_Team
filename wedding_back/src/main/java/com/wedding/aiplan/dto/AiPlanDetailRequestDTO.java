package com.wedding.aiplan.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// "자세히" 모드 - 공통 필수 4개 + 카테고리별 선호(문서 2번 표)
// hallType은 com.wedding.company.domain.HallType enum 이름 그대로 받음 (예: "GARDEN", "HOTEL")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiPlanDetailRequestDTO {

    private Long budget;

    private String region;

    private String groomName;

    private String brideName;

    private LocalDate weddingDate;

    // 홀 분위기 (HallType enum 이름: GRAND/HOTEL/HOUSE/CONVENTION/CHAPEL/GARDEN/BANQUET)
    private String hallType;

    // 스튜디오 분위기 키워드 (예: "내추럴", "클래식", "시네마틱") - StudioDetail.themeTags 매칭
    private String studioMood;

    // 드레스 스타일 키워드 (예: "머메이드", "볼륨") - DressItem.styleTags 매칭
    private String dressStyle;

    // 메이크업 스타일 키워드 (예: "내추럴", "화사") - 구조화 필드가 없어 Company.description 매칭으로 대체
    private String makeupStyle;

    // 자유 입력 텍스트 (문서 2번: 추가 요청사항) - 아직 AI 호출 전이라 스코어링엔 안 쓰이고 세션 저장용으로만 받아둠
    private String freeText;
}
