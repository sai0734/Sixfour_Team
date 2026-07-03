package com.wedding.reservation.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import com.wedding.reservation.domain.Reservation;
import com.wedding.reservation.dto.ReservationDTO;
import com.wedding.reservation.repository.ReservationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class ReservationServiceImpl implements ReservationService {

  private final ReservationRepository reservationRepository;

  private final ModelMapper modelMapper;

  @Override
  public Long register(ReservationDTO reservationDTO) {

    log.info("reservation register.........");

    Reservation reservation = modelMapper.map(reservationDTO, Reservation.class);

    Reservation saved = reservationRepository.save(reservation);

    return saved.getReservationId();
  }

  @Override
  public ReservationDTO get(Long reservationId) {

    Optional<Reservation> result = reservationRepository.findById(reservationId);

    Reservation reservation = result.orElseThrow();

    return modelMapper.map(reservation, ReservationDTO.class);
  }

  @Override
  public void modify(ReservationDTO reservationDTO) {

    Optional<Reservation> result =
            reservationRepository.findById(reservationDTO.getReservationId());

    Reservation reservation = result.orElseThrow();

    reservation.changeWeddingDate(reservationDTO.getWeddingDate());
    reservation.changeStatus(reservationDTO.getStatus());
    reservation.changeMemo(reservationDTO.getMemo());

    reservationRepository.save(reservation);
  }

  @Override
  public void remove(Long reservationId) {

    reservationRepository.deleteById(reservationId);
  }

  @Override
  public List<ReservationDTO> listByMember(String memberEmail) {

    List<Reservation> result =
            reservationRepository.findByMemberEmailOrderByReservationIdDesc(memberEmail);

    return result.stream()
            .map(r -> modelMapper.map(r, ReservationDTO.class))
            .collect(Collectors.toList());
  }

}
