package com.wedding.company.repository;

import com.wedding.company.domain.MakeupDetail;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MakeupDetailRepository extends JpaRepository<MakeupDetail, Long> {
  Optional<MakeupDetail> findByCompany_Cmno(Long cmno);
}
