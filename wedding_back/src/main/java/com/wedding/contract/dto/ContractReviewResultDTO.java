package com.wedding.contract.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContractReviewResultDTO {

    private boolean looksLikeContract; // 애초에 계약서로 보이는 문서가 맞는지 (엉뚱한 파일 방지)

    private String overallRisk;        // HIGH / MEDIUM / LOW / SAFE

    private String summary;            // 한두 문장 총평

    private List<ContractClauseFlagDTO> flags; // 발견된 독소조항 목록

}