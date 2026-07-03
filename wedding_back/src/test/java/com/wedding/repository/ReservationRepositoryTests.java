package com.wedding.repository;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.wedding.reservation.domain.Reservation;
import com.wedding.reservation.repository.ReservationRepository;

import lombok.extern.log4j.Log4j2;

@SpringBootTest
@Log4j2
public class ReservationRepositoryTests {

    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    public void test1(){

        log.info("----------------------------");   
        log.info(reservationRepository);
    }

  @Test
  public void testInsert() {

    for (int i = 1; i <= 100; i++) {

      Reservation reservation = Reservation.builder()
      .title("Title..." + i)
      .dueDate(LocalDate.of(2024,12,31))
      .writer("user00")
      .build();

      reservationRepository.save(reservation);
    }
  }

    @Test
  public void testModify() {

    Long reservationId = 100L;

    java.util.Optional<Reservation> result = reservationRepository.findById(reservationId); //java.util 패키지의 Optional

    Reservation reservation = result.orElseThrow();
    reservation.changeTitle("Modified 100...");
    reservation.changeComplete(true);
    reservation.changeDueDate(LocalDate.of(2026,06,23));

    reservationRepository.save(reservation);

  }

      @Test
  public void testPaging() {

    //import org.springframework.data.domain.Pageable;

    Pageable pageable = PageRequest.of(0,10, Sort.by("reservationId").descending());

    Page<Reservation> result = reservationRepository.findAll(pageable);

    log.info(result.getTotalElements());

    result.getContent().stream().forEach(reservation -> log.info(reservation));

  }
    
}