package com.wedding.ddayevent.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import com.wedding.ddayevent.domain.DdayEvent;
import com.wedding.ddayevent.dto.DdayEventDTO;
import com.wedding.ddayevent.repository.DdayEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class DdayEventServiceImpl implements DdayEventService {

    private final DdayEventRepository ddayEventRepository;

    private final ModelMapper modelMapper;

    @Override
    public Long register(DdayEventDTO ddayEventDTO) {

        log.info("ddayEvent register.........");

        DdayEvent ddayEvent = modelMapper.map(ddayEventDTO, DdayEvent.class);

        DdayEvent saved = ddayEventRepository.save(ddayEvent);

        return saved.getDdayId();
    }

    @Override
    public void modify(DdayEventDTO ddayEventDTO) {

        Optional<DdayEvent> result = ddayEventRepository.findById(ddayEventDTO.getDdayId());

        DdayEvent ddayEvent = result.orElseThrow();

        ddayEvent.changeTitle(ddayEventDTO.getTitle());
        ddayEvent.changeEventDate(ddayEventDTO.getEventDate());
        ddayEvent.changeMemo(ddayEventDTO.getMemo());

        ddayEventRepository.save(ddayEvent);
    }

    @Override
    public void remove(Long ddayId) {

        ddayEventRepository.deleteById(ddayId);
    }

    @Override
    public List<DdayEventDTO> listByMember(String memberEmail) {

        List<DdayEvent> result = ddayEventRepository.findByMemberEmailOrderByEventDateAsc(memberEmail);

        return result.stream()
                .map(d -> modelMapper.map(d, DdayEventDTO.class))
                .collect(Collectors.toList());
    }

}
