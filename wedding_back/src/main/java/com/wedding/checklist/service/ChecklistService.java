package com.wedding.checklist.service;

import java.util.List;

import com.wedding.checklist.dto.ChecklistDTO;

public interface ChecklistService {

    Long register(ChecklistDTO checklistDTO);

    ChecklistDTO get(Long checklistId);

    void modify(ChecklistDTO checklistDTO, String requesterEmail);

    void remove(Long checklistId, String requesterEmail);

    // 페이징 없이 회원의 체크리스트 전체를 가져오는 용도 (목록이 보통 적기 때문)
    List<ChecklistDTO> listByMember(String memberEmail);

}
