package com.wedding.company.repository;

import com.wedding.company.domain.CompanyPackage;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CompanyPackageRepository extends JpaRepository<CompanyPackage, Long> {

    @EntityGraph(attributePaths = {"hallCompany", "dressCompany", "studioCompany", "makeupCompany"})
    @Query("select p from CompanyPackage p where p.delFlag = false order by p.pno desc")
    List<CompanyPackage> findAllActive();

    @EntityGraph(attributePaths = {"hallCompany", "dressCompany", "studioCompany", "makeupCompany"})
    @Query("select p from CompanyPackage p where p.delFlag = false order by p.purchaseCount desc")
    List<CompanyPackage> findAllActiveOrderByPurchaseCountDesc();

    @EntityGraph(attributePaths = {"hallCompany", "dressCompany", "studioCompany", "makeupCompany"})
    @Query("select p from CompanyPackage p where p.delFlag = false order by p.distanceKm asc")
    List<CompanyPackage> findAllActiveOrderByDistanceAsc();
}