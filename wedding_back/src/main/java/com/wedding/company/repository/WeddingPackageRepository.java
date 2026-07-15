package com.wedding.company.repository;

import com.wedding.company.domain.WeddingPackage;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WeddingPackageRepository extends JpaRepository<WeddingPackage, Long> {

    @Query("""
      select p from WeddingPackage p
      where p.delFlag = false
        and (:keyword is null
             or p.name like concat('%', :keyword, '%')
             or p.description like concat('%', :keyword, '%'))
      """)
    Page<WeddingPackage> searchList(@Param("keyword") String keyword, Pageable pageable);

    @EntityGraph(attributePaths = "items")
    @Query("select p from WeddingPackage p where p.weddingPackageId = :id and p.delFlag = false")
    Optional<WeddingPackage> selectOne(@Param("id") Long id);
}