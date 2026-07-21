package com.wedding.aidress.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TryOnHistoryDTO {

  private Long historyId;
  private Long dressItemId;
  private String dressName;
  private String resultImageUrl;
  private String tryOnImageUrl;
  private String backgroundPrompt;
  private LocalDateTime createdAt;
}
