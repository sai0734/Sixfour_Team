package com.wedding.reservation.service;

import java.util.List;

import com.wedding.reservation.dto.ReservationDTO;

public interface ReservationService {

    Long register(ReservationDTO reservationDTO);

    ReservationDTO get(Long reservationId);

    void modify(ReservationDTO reservationDTO);

    void remove(Long reservationId);

    List<ReservationDTO> listByMember(String memberEmail);

}
