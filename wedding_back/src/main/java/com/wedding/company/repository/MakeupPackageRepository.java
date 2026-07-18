package com.wedding.company.repository;

import com.wedding.company.domain.MakeupPackage;
import com.wedding.company.domain.MakeupPackageType;
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

  // 여러 패키지 타입 중 하나라도 보유한 업체 번호 목록 (예: HAIR_NAIL을 찾을 때 이를 포함하는 FULL도 같이 매칭)
  @Query("select distinct mp.company.cmno from MakeupPackage mp "
      + "where mp.packageType in :packageTypes and mp.company.delFlag = false")
  List<Long> findCompanyCmnosByPackageTypeIn(@Param("packageTypes") List<MakeupPackageType> packageTypes);
}
