package com.wedding.company.repository;

import com.wedding.company.domain.DressDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DressDetailRepository extends JpaRepository<DressDetail, Long> {

  Optional<DressDetail> findByCompany_Cno(Long cno);
}
