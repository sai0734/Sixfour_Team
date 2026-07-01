package com.wedding.company.repository;

import com.wedding.company.domain.HallDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HallDetailRepository extends JpaRepository<HallDetail, Long> {

  Optional<HallDetail> findByCompany_Cno(Long cno);
}
