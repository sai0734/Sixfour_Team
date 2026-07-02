package com.wedding.company.repository;

import com.wedding.company.domain.DressItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DressItemRepository extends JpaRepository<DressItem, Long> {
  List<DressItem> findByCompany_CmnoOrderByOrdAsc(Long cmno);
  void deleteByCompany_Cmno(Long cmno);
}
