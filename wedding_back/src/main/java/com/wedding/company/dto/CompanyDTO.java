package com.wedding.company.dto;

import com.wedding.company.domain.CompanyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompanyDTO {

  private Long cno;

  private String name;

  private String ceoName;

  private CompanyType type;

  private String region;

  private String address;

  private Double latitude;

  private Double longitude;

  private String phone;

  private Integer price;

  private String description;

  private double ratingAvg;

  private int reviewCount;

  private Long viewCount;

  private boolean delFlag;

  @Builder.Default
  private List<String> uploadFileNames = new ArrayList<>();

  private HallDetailDTO hallDetail;

  private DressDetailDTO dressDetail;

  private MakeupDetailDTO makeupDetail;

  private StudioDetailDTO studioDetail;
}
