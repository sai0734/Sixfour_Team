package com.wedding.company.repository;

import com.wedding.company.domain.DressItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DressItemRepository extends JpaRepository<DressItem, Long> {
  List<DressItem> findByCompany_CmnoOrderByOrdAsc(Long cmno);

  @Modifying
  @Query("DELETE FROM DressItem d WHERE d.company.cmno = :cmno")
  void deleteByCompany_Cmno(@Param("cmno") Long cmno);
}
