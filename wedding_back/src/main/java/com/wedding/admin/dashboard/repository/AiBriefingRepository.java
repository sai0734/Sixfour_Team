package com.wedding.admin.dashboard.repository;

import com.wedding.admin.dashboard.domain.AiBriefing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiBriefingRepository extends JpaRepository<AiBriefing, Long> {

    List<AiBriefing> findAllByOrderByRegDateDesc();
}
