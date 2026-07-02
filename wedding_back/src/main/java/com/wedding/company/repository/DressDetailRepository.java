package com.wedding.company.repository;

import com.wedding.company.domain.DressDetail;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DressDetailRepository extends JpaRepository<DressDetail, Long> {
  Optional<DressDetail> findByCompany_Cmno(Long cmno);
}
