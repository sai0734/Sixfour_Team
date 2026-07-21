package com.wedding.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.wedding.checklist.dto.ChecklistDTO;
import com.wedding.checklist.service.ChecklistService;

import lombok.extern.log4j.Log4j2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 체크리스트가 예약 자동 생성분 말고는 전부 사용자가 직접 입력해야 해서 불편하다는 피드백으로,
// 항목이 하나도 없는 회원이 처음 조회하면 기본 항목들을 미리 채워주도록 바꿨다
// (ChecklistServiceImpl.listByMember). 기본 항목이 3단계에 걸쳐 들어가는지, 이미 항목이 있으면
// 다시 채우지 않는지(중복 방지) 확인한다. 롤백 기본값이라 테스트 끝나면 DB에 안 남는다.
@SpringBootTest
@Log4j2
public class ChecklistDefaultSeedTests {

    @Autowired
    private ChecklistService checklistService;

    // 실제 개발 DB에 대고 도는 테스트라, 기존 회원 이메일과 겹치면 이미 항목이 있어서
    // "비어있음" 전제가 깨질 수 있다 - 충돌 가능성이 거의 없는 전용 이메일을 씀.
    private static final String TEST_EMAIL = "checklist-seed-test@test.com";

    @Transactional
    @Test
    public void testEmptyChecklistGetsSeededWithDefaults() {

        List<ChecklistDTO> result = checklistService.listByMember(TEST_EMAIL);

        log.info("seeded checklist: {}", result);

        assertTrue(result.size() > 0, "빈 상태에서 조회하면 기본 항목이 채워져야 함");

        Map<Integer, Long> countByStage = result.stream()
                .collect(Collectors.groupingBy(ChecklistDTO::getStage, Collectors.counting()));

        assertTrue(countByStage.containsKey(1), "1단계(기본 계획) 기본 항목이 있어야 함");
        assertTrue(countByStage.containsKey(2), "2단계(업체 계약) 기본 항목이 있어야 함");
        assertTrue(countByStage.containsKey(3), "3단계(청첩장·답례품) 기본 항목이 있어야 함");
    }

    @Transactional
    @Test
    public void testSecondCallDoesNotDuplicateDefaults() {

        List<ChecklistDTO> first = checklistService.listByMember(TEST_EMAIL);
        List<ChecklistDTO> second = checklistService.listByMember(TEST_EMAIL);

        log.info("first={}, second={}", first.size(), second.size());

        assertEquals(first.size(), second.size(), "이미 항목이 있으면 다시 채우면 안 됨(중복 방지)");
    }
}
