package com.wedding.service;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.wedding.global.dto.PageRequestDTO;
import com.wedding.global.dto.PageResponseDTO;
import com.wedding.reservation.dto.ReservationDTO;
import com.wedding.reservation.service.ReservationService;

import lombok.extern.log4j.Log4j2;

@SpringBootTest
@Log4j2
public class ReservationServiceTests {
  
  @Autowired
  private ReservationService reservationService;

  @Test
  public void testRegister() {

    ReservationDTO reservationDTO = ReservationDTO.builder()
    .title("서비스 테스트")
    .writer("tester")
    .dueDate(LocalDate.of(2026,10,10))
    .build();

    Long reservationId = reservationService.register(reservationDTO);

    log.info("TNO: " + reservationId);
    
  }

   @Test
  public void testList() {
    PageRequestDTO pageRequestDTO = PageRequestDTO.builder()
    .page(2)
    .size(10)
    .build();
    PageResponseDTO<ReservationDTO> response = reservationService.list(pageRequestDTO);
    log.info(response);
  }
}