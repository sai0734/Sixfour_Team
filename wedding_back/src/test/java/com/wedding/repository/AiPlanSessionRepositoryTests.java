package com.wedding.repository;

import java.time.LocalDate;
import java.util.Optional;

import com.wedding.aiplan.repository.AiPlanSessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.wedding.aiplan.domain.AiPlanSession;
import com.wedding.aiplan.domain.SlotState;
import com.wedding.aiplan.domain.SlotStatus;

import lombok.extern.log4j.Log4j2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SpringBootTest
@Log4j2
public class AiPlanSessionRepositoryTests {

    @Autowired
    private AiPlanSessionRepository aiPlanSessionRepository;

    // 4개 슬롯에 서로 다른 값을 넣고 저장 -> 다시 조회했을 때 슬롯끼리 안 섞였는지 확인.
    // @AttributeOverrides 컬럼 매핑이 잘못되면(접두어 없이 공유되면) 여기서 걸림.
    // 롤백 기본값이라 테스트 끝나면 DB에 안 남음.
    @Transactional
    @Test
    public void testSlotsDoNotOverlap() {

        AiPlanSession session = AiPlanSession.builder()
                .memberEmail("test@test.com")
                .budget(30_000_000L)
                .region("강남")
                .weddingDate(LocalDate.of(2027, 5, 1))
                .mode("DETAIL")
                .hallSlot(SlotState.builder()
                        .status(SlotStatus.CONFIRMED)
                        .selectedCmno(101L)
                        .note("홀 메모")
                        .build())
                .studioSlot(SlotState.builder()
                        .status(SlotStatus.EXCLUDED)
                        .selectedCmno(202L)
                        .note("스튜디오 메모")
                        .build())
                .dressSlot(SlotState.builder()
                        .status(SlotStatus.PENDING)
                        .selectedCmno(303L)
                        .note("드레스 메모")
                        .build())
                .makeupSlot(SlotState.builder()
                        .status(SlotStatus.CONFIRMED)
                        .selectedCmno(404L)
                        .note("메이크업 메모")
                        .build())
                .build();

        AiPlanSession saved = aiPlanSessionRepository.saveAndFlush(session);

        Optional<AiPlanSession> result = aiPlanSessionRepository.findById(saved.getSessionId());
        AiPlanSession found = result.orElseThrow();

        log.info(found);

        // 4개 슬롯의 selectedCmno가 서로 안 겹치는지 (컬럼이 공유됐다면 전부 마지막 값인 404로 덮였을 것)
        assertEquals(101L, found.getHallSlot().getSelectedCmno());
        assertEquals(202L, found.getStudioSlot().getSelectedCmno());
        assertEquals(303L, found.getDressSlot().getSelectedCmno());
        assertEquals(404L, found.getMakeupSlot().getSelectedCmno());

        // status도 4개 다 다르게 넣었으니 서로 달라야 정상
        assertEquals(SlotStatus.CONFIRMED, found.getHallSlot().getStatus());
        assertEquals(SlotStatus.EXCLUDED, found.getStudioSlot().getStatus());
        assertEquals(SlotStatus.PENDING, found.getDressSlot().getStatus());
        assertNotEquals(found.getHallSlot().getStatus(), found.getStudioSlot().getStatus());

        // note도 슬롯별로 따로 저장되는지
        assertEquals("홀 메모", found.getHallSlot().getNote());
        assertEquals("스튜디오 메모", found.getStudioSlot().getNote());
    }

}
