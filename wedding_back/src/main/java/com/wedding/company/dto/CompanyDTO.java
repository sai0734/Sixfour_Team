package com.wedding.company.dto;

import com.wedding.company.domain.CompanyCategory;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompanyDTO {

  private Long cmno;
  private CompanyCategory category;
  private String name;
  private String ceoName;
  private String phone;
  private String address;
  private Double latitude;
  private Double longitude;
  private String description;
  private BigDecimal priceAvg;
  private boolean delFlag;

  @Builder.Default
  private List<String> uploadFileNames = new ArrayList<>();

  private HallDetailDTO hallDetail;
  private DressDetailDTO dressDetail;
  private MakeupDetailDTO makeupDetail;
  private StudioDetailDTO studioDetail;
}
