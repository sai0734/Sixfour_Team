package com.wedding.ddayevent.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wedding.ddayevent.domain.DdayEvent;

public interface DdayEventRepository extends JpaRepository<DdayEvent, Long> {

    List<DdayEvent> findByMemberEmailOrderByEventDateAsc(String memberEmail);

}
