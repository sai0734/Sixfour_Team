package com.wedding.reservation.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wedding.reservation.domain.Reservation;
import com.wedding.global.dto.PageRequestDTO;
import com.wedding.global.dto.PageResponseDTO;
import com.wedding.reservation.dto.ReservationDTO;
import com.wedding.reservation.repository.ReservationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Transactional
@Log4j2
@RequiredArgsConstructor  // 생성자 자동 주입
public class ReservationServiceImpl implements ReservationService {

  //자동주입 대상은 final로 
  private final ModelMapper modelMapper;

  private final ReservationRepository reservationRepository;

  @Override
  public Long register(ReservationDTO reservationDTO) {
    
    log.info(".........");

    Reservation reservation = modelMapper.map(reservationDTO, Reservation.class);

    Reservation savedReservation = reservationRepository.save(reservation);

    return savedReservation.getReservationId();

  }

    @Override
  public ReservationDTO get(Long reservationId) {
    
    java.util.Optional<Reservation> result = reservationRepository.findById(reservationId);

    Reservation reservation = result.orElseThrow();

    ReservationDTO dto = modelMapper.map(reservation, ReservationDTO.class);

    return dto;
  }
    @Override
  public void modify(ReservationDTO reservationDTO) {

    Optional<Reservation> result = reservationRepository.findById(reservationDTO.getReservationId());

    Reservation reservation = result.orElseThrow();

    reservation.changeTitle(reservationDTO.getTitle());
    reservation.changeDueDate(reservationDTO.getDueDate());
    reservation.changeComplete(reservationDTO.isComplete());
 
    reservationRepository.save(reservation);

  }

  @Override
  public void remove(Long reservationId) {
    
    reservationRepository.deleteById(reservationId);

  }

    @Override
  public PageResponseDTO<ReservationDTO> list(PageRequestDTO pageRequestDTO) {

    Pageable pageable = 
      PageRequest.of( 
        pageRequestDTO.getPage() - 1 ,  // 1페이지가 0이므로 주의 
        pageRequestDTO.getSize(), 
        Sort.by("reservationId").descending());

    Page<Reservation> result = reservationRepository.findAll(pageable);    

    List<ReservationDTO> dtoList = result.getContent().stream()
      .map(reservation -> modelMapper.map(reservation, ReservationDTO.class))
      .collect(Collectors.toList());
    
    long totalCount = result.getTotalElements();

    PageResponseDTO<ReservationDTO> responseDTO = PageResponseDTO.<ReservationDTO>withAll()
      .dtoList(dtoList)
      .pageRequestDTO(pageRequestDTO)
      .totalCount(totalCount)
      .build();

    return responseDTO;
  }
}