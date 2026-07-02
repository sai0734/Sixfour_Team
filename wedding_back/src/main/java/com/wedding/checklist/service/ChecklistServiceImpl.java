package com.wedding.checklist.service;

import java.util.List;
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
    public void modify(ChecklistDTO checklistDTO) {

        Optional<Checklist> result = checklistRepository.findById(checklistDTO.getChecklistId());

        Checklist checklist = result.orElseThrow();

        checklist.changeTitle(checklistDTO.getTitle());
        checklist.changeIsDone(checklistDTO.isDone());
        checklist.changeDueDate(checklistDTO.getDueDate());
        checklist.changeSortOrder(checklistDTO.getSortOrder());
        checklist.changeStage(checklistDTO.getStage());

        checklistRepository.save(checklist);
    }

    @Override
    public void remove(Long checklistId) {

        checklistRepository.deleteById(checklistId);
    }

    @Override
    public List<ChecklistDTO> listByMember(String memberEmail) {

        List<Checklist> result = checklistRepository.findByMemberEmailOrderByStageAscSortOrderAsc(memberEmail);

        return result.stream()
                .map(checklist -> modelMapper.map(checklist, ChecklistDTO.class))
                .collect(Collectors.toList());
    }

}
