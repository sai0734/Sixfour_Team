package com.wedding.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedding.company.domain.MakeupDetail;
import com.wedding.company.domain.MakeupPackage;
import com.wedding.company.domain.MakeupPackageType;
import com.wedding.company.repository.MakeupDetailRepository;
import com.wedding.company.repository.MakeupPackageRepository;

import lombok.extern.log4j.Log4j2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 버그 리포트: 메이크업 풀 패키지(734,800원)를 추천해놓고 최종 조합 합계에는 업체 평균가
// (278,333원)가 더해짐 - makeupPrice/packagePrice가 그라운딩된 makeupPackageType을 무시하고
// 항상 company.priceAvg()만 쓰고 있었음(AiPlanCandidateBuilder.resolveMakeupPrice로 수정).
// 실제 반환된 makeupCmno의 MakeupDetail(단품가)+MakeupPackage(할인율)로 기대값을 직접 계산해서
// makeupPrice가 그 값과 일치하는지, 그리고 (할인율이 있다면) priceAvg와는 다른지 확인한다.
// 롤백 기본값이라 테스트 끝나면 DB에 안 남는다.
@SpringBootTest
@AutoConfigureMockMvc
@Log4j2
public class AiPlanMakeupPriceAccuracyTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MakeupDetailRepository makeupDetailRepository;

    @Autowired
    private MakeupPackageRepository makeupPackageRepository;

    @Test
    @Transactional
    public void testMakeupPriceReflectsActualPackagePriceNotAverage() throws Exception {

        String response = mockMvc.perform(get("/api/aiplan/detail")
                        .param("budget", "300000000")
                        .param("region", "강남")
                        .param("makeupStyle", "FULL"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> body = objectMapper.readValue(response, Map.class);
        java.util.List<?> candidates = (java.util.List<?>) body.get("candidates");
        Map<?, ?> combo = (Map<?, ?>) candidates.get(0);

        assertEquals("FULL", combo.get("makeupPackageType"), "FULL 취향이 그라운딩돼야 함");

        Long makeupCmno = ((Number) combo.get("makeupCmno")).longValue();
        BigDecimal actualMakeupPrice = new BigDecimal(combo.get("makeupPrice").toString());

        MakeupDetail detail = makeupDetailRepository.findByCompany_Cmno(makeupCmno).orElseThrow();
        BigDecimal basePrice = detail.getHairPrice().add(detail.getMakeupPrice()).add(detail.getNailPrice());

        BigDecimal discountRate = makeupPackageRepository.findByCompany_Cmno(makeupCmno).stream()
                .filter(p -> p.getPackageType() == MakeupPackageType.FULL)
                .map(MakeupPackage::getDiscountRate)
                .findFirst()
                .orElse(BigDecimal.ZERO);
        if (discountRate.compareTo(BigDecimal.ONE) > 0) {
            discountRate = discountRate.divide(BigDecimal.valueOf(100));
        }

        BigDecimal expectedPrice = basePrice.multiply(BigDecimal.ONE.subtract(discountRate))
                .setScale(0, RoundingMode.HALF_UP);

        log.info("makeupCmno={}, actualMakeupPrice={}, expectedPrice(hair+makeup+nail-discount)={}",
                makeupCmno, actualMakeupPrice, expectedPrice);

        assertEquals(0, expectedPrice.compareTo(actualMakeupPrice),
                "makeupPrice가 풀 패키지 실제가(단품가 합 - 할인율)와 일치해야 함");

        if (discountRate.signum() > 0) {
            assertNotEquals(0, basePrice.compareTo(actualMakeupPrice),
                    "할인율이 있으면 makeupPrice가 단순 합계와는 달라야 함(할인 미반영 회귀 방지)");
        }
    }
}
