package com.wedding.reservation.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wedding.reservation.dto.ReservationDTO;
import com.wedding.reservation.service.ReservationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/reservations")
public class ReservationController {

  private final ReservationService service;

  @GetMapping("/{reservationId}")
  public ReservationDTO get(@PathVariable(name = "reservationId") Long reservationId) {

    return service.get(reservationId);
  }

  // 회원의 예약 전체 조회 (마이페이지 "예약 현황" 탭용)
  @GetMapping("/member/{memberEmail}")
  public List<ReservationDTO> listByMember(@PathVariable(name = "memberEmail") String memberEmail) {

    return service.listByMember(memberEmail);
  }

  // D파트 업체탐색 "예약하기" 버튼에서도 이 엔드포인트를 그대로 호출
  @PostMapping("/")
  public Map<String, Long> register(@RequestBody ReservationDTO reservationDTO) {

    log.info("ReservationDTO: " + reservationDTO);

    Long reservationId = service.register(reservationDTO);

    return Map.of("reservationId", reservationId);
  }

  @PutMapping("/{reservationId}")
  public Map<String, String> modify(
          @PathVariable(name = "reservationId") Long reservationId,
          @RequestBody ReservationDTO reservationDTO) {

    reservationDTO.setReservationId(reservationId);

    log.info("Modify: " + reservationDTO);

    service.modify(reservationDTO);

    return Map.of("RESULT", "SUCCESS");
  }

  @DeleteMapping("/{reservationId}")
  public Map<String, String> remove(@PathVariable(name = "reservationId") Long reservationId) {

    log.info("Remove: " + reservationId);

    service.remove(reservationId);

    return Map.of("RESULT", "SUCCESS");
  }

}
