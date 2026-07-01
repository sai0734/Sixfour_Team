package com.wedding.company.repository;

import com.wedding.company.domain.StudioDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudioDetailRepository extends JpaRepository<StudioDetail, Long> {

  Optional<StudioDetail> findByCompany_Cno(Long cno);
}
