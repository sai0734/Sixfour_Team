package com.wedding.reservation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wedding.reservation.domain.Reservation;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByMemberEmailOrderByReservationIdDesc(String memberEmail);

}
