package com.wedding.reservation.service;

import com.wedding.global.dto.PageRequestDTO;
import com.wedding.global.dto.PageResponseDTO;
import com.wedding.reservation.dto.ReservationDTO;

public interface ReservationService {
    
    Long register(ReservationDTO reservationDTO);

    ReservationDTO get(Long reservationId);

    void modify(ReservationDTO reservationDTO);

    void remove(Long reservationId);

     PageResponseDTO<ReservationDTO> list(PageRequestDTO pageRequestDTO);
}