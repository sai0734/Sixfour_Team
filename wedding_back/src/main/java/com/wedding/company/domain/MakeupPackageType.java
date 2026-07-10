package com.wedding.company.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum MakeupPackageType {
  HAIR,
  MAKEUP,
  NAIL,
  HAIR_MAKEUP,
  HAIR_NAIL,
  MAKEUP_NAIL,
  FULL;

  /**
   * 프론트 더미 데이터의 레거시 값(TWO, THREE 등)을 DB 표준값으로 정규화.
   * Jackson 역직렬화 시 자동 호출됨.
   */
  @JsonCreator
  public static MakeupPackageType fromString(String value) {
    if (value == null) return MAKEUP;
    return switch (value) {
      case "TWO",  "HAIR_MAKEUP" -> HAIR_MAKEUP;
      case "THREE", "NAIL_MAKEUP", "FULL" -> FULL;
      case "HAIR_ONLY", "HAIR" -> HAIR;
      case "MAKEUP_ONLY", "MAKEUP" -> MAKEUP;
      case "NAIL" -> NAIL;
      case "HAIR_NAIL" -> HAIR_NAIL;
      case "MAKEUP_NAIL" -> MAKEUP_NAIL;
      default -> MakeupPackageType.valueOf(value);
    };
  }
}
