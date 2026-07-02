package com.wedding.company.repository;

import com.wedding.company.domain.HallItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HallItemRepository extends JpaRepository<HallItem, Long> {
  List<HallItem> findByCompany_CmnoOrderByOrdAsc(Long cmno);
  void deleteByCompany_Cmno(Long cmno);
}
