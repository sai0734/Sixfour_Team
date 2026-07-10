package com.wedding.ddayevent.service;

import java.util.List;

import com.wedding.ddayevent.dto.DdayEventDTO;

public interface DdayEventService {

    Long register(DdayEventDTO ddayEventDTO);

    void modify(DdayEventDTO ddayEventDTO, String requesterEmail);

    void remove(Long ddayId, String requesterEmail);

    List<DdayEventDTO> listByMember(String memberEmail);

}
