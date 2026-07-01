package com.wedding.company.repository;

import com.wedding.company.domain.MakeupDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MakeupDetailRepository extends JpaRepository<MakeupDetail, Long> {

  Optional<MakeupDetail> findByCompany_Cno(Long cno);
}
