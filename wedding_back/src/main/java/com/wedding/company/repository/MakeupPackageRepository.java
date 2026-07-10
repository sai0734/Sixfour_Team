package com.wedding.company.repository;

import com.wedding.company.domain.MakeupPackage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MakeupPackageRepository extends JpaRepository<MakeupPackage, Long> {
  List<MakeupPackage> findByCompany_Cmno(Long cmno);

  @Modifying
  @Query("DELETE FROM MakeupPackage p WHERE p.company.cmno = :cmno")
  void deleteByCompany_Cmno(@Param("cmno") Long cmno);
}
