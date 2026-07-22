package com.wedding.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedding.company.domain.MakeupDetail;
import com.wedding.company.repository.MakeupDetailRepository;

import lombok.extern.log4j.Log4j2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 버그 리포트: 메이크업 취향으로 FULL(풀세트)을 고르면 makeupPackageType/가격이 잘 나오는데,
// 단품(HAIR 등)이나 2개 조합(HAIR_NAIL 등)을 고르면 makeupPackageType이 null로 빠지고 가격도
// 업체 평균가로 폴백됐음. 원인은 AiPlanCandidateBuilder.findStyledMakeup 등이 "이 타입을 파는
// 업체"를 MakeupPackage(할인 패키지) row 존재 여부로만 판단했기 때문 - 더미데이터엔 보통 FULL/
// HAIR_MAKEUP처럼 할인이 붙는 조합만 MakeupPackage row가 있고, 단품·무할인 조합은 row가 없어서
// "그 타입을 아예 안 판다"고 잘못 판단했음. MakeupDetail.supports()(취급 서비스 기준)로 바꿔서
// 고침. 취향을 아예 안 줬을 때도(랜덤 추천) makeupPackageType이 null로 비지 않고 그 업체가 실제
// 파는 것 중 가장 풍성한 조합(bestMakeupType)으로 채워지는지도 같이 확인한다.
// 롤백 기본값이라 테스트 끝나면 DB에 안 남는다.
@SpringBootTest
@AutoConfigureMockMvc
@Log4j2
public class AiPlanMakeupSingleAndPairPriceTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MakeupDetailRepository makeupDetailRepository;

    @Test
    @Transactional
    public void testSingleHairStyleGetsGroundedTypeAndCorrectPrice() throws Exception {
        assertGroundedAndPriceCorrect("HAIR");
    }

    @Test
    @Transactional
    public void testPairHairNailStyleGetsGroundedTypeAndCorrectPrice() throws Exception {
        assertGroundedAndPriceCorrect("HAIR_NAIL");
    }

    @Test
    @Transactional
    public void testNoPreferenceStillFillsBestAvailablePackageType() throws Exception {

        String response = mockMvc.perform(get("/api/aiplan/detail")
                        .param("budget", "300000000")
                        .param("region", "강남"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> body = objectMapper.readValue(response, Map.class);
        Map<?, ?> combo = (Map<?, ?>) ((List<?>) body.get("candidates")).get(0);

        log.info("no-preference makeupPackageType={}, makeupPrice={}",
                combo.get("makeupPackageType"), combo.get("makeupPrice"));

        assertNotNull(combo.get("makeupPackageType"),
                "취향을 안 줘도(랜덤 추천) 그 업체가 실제 파는 것 중 가장 풍성한 패키지로 채워져야 함");
    }

    private void assertGroundedAndPriceCorrect(String style) throws Exception {

        String response = mockMvc.perform(get("/api/aiplan/detail")
                        .param("budget", "300000000")
                        .param("region", "강남")
                        .param("makeupStyle", style))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> body = objectMapper.readValue(response, Map.class);
        Map<?, ?> combo = (Map<?, ?>) ((List<?>) body.get("candidates")).get(0);

        Long makeupCmno = ((Number) combo.get("makeupCmno")).longValue();
        Object actualType = combo.get("makeupPackageType");
        BigDecimal actualPrice = new BigDecimal(combo.get("makeupPrice").toString());

        log.info("style={}, makeupCmno={}, actualType={}, actualPrice={}",
                style, makeupCmno, actualType, actualPrice);

        assertEquals(style, actualType, style + " 취향을 줬으면 makeupPackageType이 그대로 그라운딩돼야 함");

        MakeupDetail detail = makeupDetailRepository.findByCompany_Cmno(makeupCmno).orElseThrow();
        BigDecimal expectedBase = switch (style) {
            case "HAIR" -> detail.getHairPrice();
            case "HAIR_NAIL" -> detail.getHairPrice().add(detail.getNailPrice());
            default -> throw new IllegalArgumentException("test only covers HAIR/HAIR_NAIL");
        };

        // 할인율이 없는 단품/무할인 조합은 단가 합과 정확히 같아야 하고, 있다면 그보다 작아야 한다.
        assertNotNull(actualPrice);
        assertTrue(actualPrice.compareTo(expectedBase) <= 0,
                "가격이 단가 합(할인 전)보다 커서는 안 됨: actual=" + actualPrice + ", base=" + expectedBase);
    }
}
