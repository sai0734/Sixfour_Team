package com.wedding.company.repository;

import com.wedding.company.domain.Company;
import com.wedding.company.domain.CompanyType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

  @EntityGraph(attributePaths = "imageList")
  @Query("select c from Company c where c.cno = :cno")
  Optional<Company> selectOne(@Param("cno") Long cno);

  @EntityGraph(attributePaths = "imageList")
  @Query("""
      select c
      from Company c
      where c.delFlag = false
        and (:type is null or c.type = :type)
        and (:region is null or c.region like concat('%', :region, '%'))
        and (:keyword is null or c.name like concat('%', :keyword, '%')
          or c.description like concat('%', :keyword, '%'))
        and (:minPrice is null or c.price >= :minPrice)
        and (:maxPrice is null or c.price <= :maxPrice)
      """)
  Page<Company> searchList(@Param("type") CompanyType type,
                           @Param("region") String region,
                           @Param("keyword") String keyword,
                           @Param("minPrice") Integer minPrice,
                           @Param("maxPrice") Integer maxPrice,
                           Pageable pageable);

  @Modifying
  @Query("update Company c set c.delFlag = :flag where c.cno = :cno")
  void updateToDelete(@Param("cno") Long cno, @Param("flag") boolean flag);
}
