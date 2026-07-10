package com.wedding.company.repository;

import com.wedding.company.domain.StudioDetail;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudioDetailRepository extends JpaRepository<StudioDetail, Long> {
  Optional<StudioDetail> findByCompany_Cmno(Long cmno);
}
