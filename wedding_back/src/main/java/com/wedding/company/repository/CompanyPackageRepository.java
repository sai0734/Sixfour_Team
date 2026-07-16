package com.wedding.company.repository;

import com.wedding.company.domain.CompanyPackage;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanyPackageRepository extends JpaRepository<CompanyPackage, Long> {

    @EntityGraph(attributePaths = {"hallCompany", "dressCompany", "studioCompany", "makeupCompany"})
    @Query("select p from CompanyPackage p where p.delFlag = false order by p.pno desc")
    List<CompanyPackage> findAllActive();

    // AI 웨딩플랜 "빠르게 모드" 지역 후보 조회 - 4개 업체 중 홀 주소 기준으로만 매칭
    // (홀이 결혼식 장소를 사실상 결정하므로). 매칭 0건이면 서비스단에서 findAllActive()로 폴백.
    @EntityGraph(attributePaths = {"hallCompany", "dressCompany", "studioCompany", "makeupCompany"})
    @Query("select p from CompanyPackage p where p.delFlag = false "
            + "and p.hallCompany.address like %:regionKeyword%")
    List<CompanyPackage> findAllActiveByHallRegion(@Param("regionKeyword") String regionKeyword);

    @EntityGraph(attributePaths = {"hallCompany", "dressCompany", "studioCompany", "makeupCompany"})
    @Query("select p from CompanyPackage p where p.delFlag = false order by p.purchaseCount desc")
    List<CompanyPackage> findAllActiveOrderByPurchaseCountDesc();

    @EntityGraph(attributePaths = {"hallCompany", "dressCompany", "studioCompany", "makeupCompany"})
    @Query("select p from CompanyPackage p where p.delFlag = false order by p.distanceKm asc")
    List<CompanyPackage> findAllActiveOrderByDistanceAsc();
}