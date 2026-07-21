package com.wedding.checklist.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wedding.checklist.domain.Checklist;
import com.wedding.checklist.dto.ChecklistDTO;
import com.wedding.checklist.repository.ChecklistRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Transactional
@Log4j2
@RequiredArgsConstructor
public class ChecklistServiceImpl implements ChecklistService {

    private final ModelMapper modelMapper;

    private final ChecklistRepository checklistRepository;

    // 체크리스트를 한 번도 안 써본 회원(항목이 아예 없음)이 처음 조회하면, 완전히 빈 화면 대신
    // 결혼 준비 과정에서 보통 필요한 기본 항목을 미리 채워서 시작하게 한다 - 예약 자동 생성
    // 항목(2단계) 말고는 전부 사용자가 직접 입력해야 해서 막상 쓰기 불편하다는 피드백 반영.
    // 별도 "seed 여부" 컬럼 없이 "지금 항목이 0개면 채운다"로 단순하게 처리한다 - 사용자가
    // 전부 지워서 의도적으로 비운 경우에도 다음 조회 때 다시 채워지는 건 감수한다.
    private static final List<Map.Entry<Integer, String>> DEFAULT_ITEMS = List.of(
            Map.entry(1, "예산 정하기"),
            Map.entry(1, "결혼식 날짜 정하기"),
            Map.entry(1, "상견례"),
            Map.entry(1, "웨딩홀·스드메 업체 탐색"),
            Map.entry(1, "신혼여행지 정하기"),
            Map.entry(2, "웨딩홀 계약"),
            Map.entry(2, "스튜디오 촬영 예약"),
            Map.entry(2, "드레스 업체 계약"),
            Map.entry(2, "메이크업 업체 예약"),
            Map.entry(2, "예복·예물 준비"),
            Map.entry(3, "청첩장 제작"),
            Map.entry(3, "청첩장 돌리기"),
            Map.entry(3, "답례품 준비"),
            Map.entry(3, "하객 명단 정리"),
            Map.entry(3, "혼인신고")
    );

    private void seedDefaults(String memberEmail) {
        Map<Integer, Integer> nextOrder = new HashMap<>();

        List<Checklist> defaults = DEFAULT_ITEMS.stream()
                .map(item -> {
                    int stage = item.getKey();
                    int order = nextOrder.merge(stage, 1, Integer::sum);
                    return Checklist.builder()
                            .memberEmail(memberEmail)
                            .title(item.getValue())
                            .stage(stage)
                            .sortOrder(order)
                            .isDone(false)
                            .build();
                })
                .toList();

        checklistRepository.saveAll(defaults);
    }

    @Override
    public Long register(ChecklistDTO checklistDTO) {

        log.info("checklist register.........");

        Checklist checklist = modelMapper.map(checklistDTO, Checklist.class);

        Checklist savedChecklist = checklistRepository.save(checklist);

        return savedChecklist.getChecklistId();
    }

    @Override
    public ChecklistDTO get(Long checklistId) {

        Optional<Checklist> result = checklistRepository.findById(checklistId);

        Checklist checklist = result.orElseThrow();

        return modelMapper.map(checklist, ChecklistDTO.class);
    }

    @Override
    public void modify(ChecklistDTO checklistDTO, String requesterEmail) {

        Optional<Checklist> result = checklistRepository.findById(checklistDTO.getChecklistId());

        Checklist checklist = result.orElseThrow();

        if (!checklist.getMemberEmail().equals(requesterEmail)) {
            throw new IllegalStateException("본인의 체크리스트 항목만 수정할 수 있습니다.");
        }

        checklist.changeTitle(checklistDTO.getTitle());
        checklist.changeIsDone(checklistDTO.isDone());
        checklist.changeDueDate(checklistDTO.getDueDate());
        checklist.changeSortOrder(checklistDTO.getSortOrder());
        checklist.changeStage(checklistDTO.getStage());

        checklistRepository.save(checklist);
    }

    @Override
    public void remove(Long checklistId, String requesterEmail) {

        Optional<Checklist> result = checklistRepository.findById(checklistId);

        Checklist checklist = result.orElseThrow();

        if (!checklist.getMemberEmail().equals(requesterEmail)) {
            throw new IllegalStateException("본인의 체크리스트 항목만 삭제할 수 있습니다.");
        }

        checklistRepository.deleteById(checklistId);
    }

    @Override
    public List<ChecklistDTO> listByMember(String memberEmail) {

        List<Checklist> result = checklistRepository.findByMemberEmailOrderByStageAscSortOrderAsc(memberEmail);

        if (result.isEmpty()) {
            seedDefaults(memberEmail);
            result = checklistRepository.findByMemberEmailOrderByStageAscSortOrderAsc(memberEmail);
        }

        return result.stream()
                .map(checklist -> modelMapper.map(checklist, ChecklistDTO.class))
                .collect(Collectors.toList());
    }

}
