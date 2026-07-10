package com.wedding.company.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DressItemType {
  DRESS,
  SUIT,
  ALINE,
  BELL,
  MERMAID,
  MINI,
  SLIM;

  @JsonCreator
  public static DressItemType fromString(String value) {
    if (value == null) return DRESS;
    return switch (value.toUpperCase()) {
      case "SUIT" -> SUIT;
      case "ALINE" -> ALINE;
      case "BELL" -> BELL;
      case "MERMAID" -> MERMAID;
      case "MINI" -> MINI;
      case "SLIM" -> SLIM;
      default -> DRESS;
    };
  }
}
