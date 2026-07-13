package com.wedding.contract.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContractClauseFlagDTO {

    private String clauseText;   // 문제가 된 조항 원문(또는 요약)

    private String riskLevel;    // HIGH / MEDIUM / LOW

    private String reason;       // 왜 소비자에게 불리한지

    private String suggestion;   // 계약 전에 이렇게 확인/요청하라는 제안

}