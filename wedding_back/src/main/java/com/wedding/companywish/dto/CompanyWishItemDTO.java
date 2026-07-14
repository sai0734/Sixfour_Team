package com.wedding.companywish.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 재원 추가 - 마이페이지 "찜 목록 > 업체 찜"에서 쓰는 표시용 DTO.
// 옵션(홀/드레스/메이크업)별로 찜한 게 여러 건일 수 있어서 wishId/optionName이 꼭 필요하고,
// 카드에 바로 보여줄 업체 정보(이름/카테고리/썸네일 등)도 같이 내려줌.
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompanyWishItemDTO {

    private Long wishId;

    private Long cmno;

    private String optionName;

    // 재원 추가 - 옵션 자체의 가격/이미지 (없으면 0 / null, 프론트에서 업체 대표가격/이미지로 대체)
    private int optionAmount;

    private String optionImage;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime regDate;

    // ↓↓↓ 업체 표시 정보 (CompanyDTO 전체를 그대로 쓰지 않고 필요한 것만)
    private String category;

    private String name;

    private String address;

    private String phone;

    private Integer priceAvg;

    private List<String> uploadFileNames;

}
