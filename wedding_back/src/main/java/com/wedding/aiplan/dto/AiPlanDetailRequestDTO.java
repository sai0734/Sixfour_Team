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

    // 하객수 - 홀 후보를 고를 때 HallDetail.maxCapacity보다 적게 수용하는 곳은 걸러냄
    // (maxCapacity가 아직 안 채워진 더미데이터는 "수용 가능"으로 간주해 안 걸러짐)
    private Integer guestCount;

    // 홀 분위기 - 칩 중복선택 결과를 콤마로 이어붙인 문자열 (예: "GARDEN,BANQUET").
    // HallType enum 이름 그대로: GRAND/HOTEL/HOUSE/CONVENTION/CHAPEL/GARDEN/BANQUET.
    // 여러 개면 그 중 하나라도 맞는 홀을 찾는다(OR) - 홀 하나에 타입은 하나뿐이라서.
    private String hallType;

    // 스튜디오 분위기 - 칩 중복선택 결과를 콤마로 이어붙인 문자열 (예: "내추럴/감성,모던/미니멀").
    // StudioDetail.themeTags 매칭. 여러 개면 전부 가진 곳을 찾는다(AND).
    private String studioMood;

    // 드레스 스타일 - 칩 중복선택 결과를 콤마로 이어붙인 문자열 (예: "머메이드,벨라인").
    // DressItem.styleTags 매칭. 여러 개면 전부 가진 아이템을 찾는다(AND).
    private String dressStyle;

    // 메이크업 패키지 타입 - com.wedding.company.domain.MakeupPackageType enum 이름 그대로 받음
    // (예: "HAIR_MAKEUP", "FULL")
    private String makeupStyle;

    // 자유 입력 텍스트 (문서 2번: 추가 요청사항) - 아직 AI 호출 전이라 스코어링엔 안 쓰이고 세션 저장용으로만 받아둠
    private String freeText;
}
