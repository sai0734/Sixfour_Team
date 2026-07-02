package com.wedding.company.repository;

import com.wedding.company.domain.HallDetail;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HallDetailRepository extends JpaRepository<HallDetail, Long> {
  Optional<HallDetail> findByCompany_Cmno(Long cmno);
}
