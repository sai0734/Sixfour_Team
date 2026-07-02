package com.wedding.company.repository;

import com.wedding.company.domain.MakeupPackage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MakeupPackageRepository extends JpaRepository<MakeupPackage, Long> {
  List<MakeupPackage> findByCompany_Cmno(Long cmno);
  void deleteByCompany_Cmno(Long cmno);
}
