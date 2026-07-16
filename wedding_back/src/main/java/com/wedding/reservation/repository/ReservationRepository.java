package com.wedding.reservation.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.wedding.company.domain.CompanyCategory;
import com.wedding.reservation.domain.Reservation;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByMemberEmailOrderByReservationIdDesc(String memberEmail);

    // 재원 추가 - 업체 상세페이지 "결제 횟수" 표시용 (인기 업체 패키지 구성 참고 지표)
    long countByCmnoAndPayStatus(Long cmno, String payStatus);

    // 재원 추가 - 같은 업체+같은 옵션+같은 예식일 예약이 이미 있는지 확인 (중복 예약 방지)
    boolean existsByCmnoAndOptionNameAndWeddingDate(
            Long cmno, String optionName, LocalDate weddingDate);

    // 승진 추가 - 업체 예약관리 목록
    List<Reservation> findByCmnoOrderByReservationIdDesc(Long cmno);

    // 관리자 대시보드용 집계
    long countByStatus(String status);

    long countByPayStatus(String payStatus);

    long countByWeddingDateBetween(LocalDate start, LocalDate end);

    // 관리자 대시보드 "업체 현황" - 카테고리(웨딩홀/드레스/스튜디오/메이크업)별 업체별 결제완료 예약 금액 합산
    // (Reservation.cmno가 연관관계 아닌 단순 Long이라 Company와 on절로 조인)
    @Query("select c.name, sum(r.amount) as total from Reservation r join Company c on r.cmno = c.cmno " +
            "where r.payStatus = 'PAID' and c.category = :category group by c.cmno, c.name order by total desc")
    List<Object[]> sumAmountByCompanyInCategoryDesc(@Param("category") CompanyCategory category, Pageable pageable);

    @Query("select c.name, sum(r.amount) as total from Reservation r join Company c on r.cmno = c.cmno " +
            "where r.payStatus = 'PAID' and c.category = :category group by c.cmno, c.name order by total asc")
    List<Object[]> sumAmountByCompanyInCategoryAsc(@Param("category") CompanyCategory category, Pageable pageable);

    @Query("select coalesce(sum(r.amount), 0) from Reservation r join Company c on r.cmno = c.cmno " +
            "where r.payStatus = 'PAID' and c.category = :category")
    long sumAmountInCategory(@Param("category") CompanyCategory category);

    @Query("select count(distinct c.cmno) from Reservation r join Company c on r.cmno = c.cmno " +
            "where r.payStatus = 'PAID' and c.category = :category")
    long countDistinctCompaniesInCategoryWithPaidReservation(@Param("category") CompanyCategory category);

    // 관리자 대시보드 "업체 매출 전체 순위" - 특정 기간(보통 1개월) 동안의 매출로 업체 랭킹.
    // category가 null이면 4개 카테고리 전체 통합 랭킹. 순위번호는 서비스단에서 매김(1위부터).
    @Query("select c.cmno, c.name, c.category, sum(r.amount) as total from Reservation r join Company c on r.cmno = c.cmno " +
            "where r.payStatus = 'PAID' and r.paidAt >= :start and r.paidAt < :end " +
            "and (:category is null or c.category = :category) " +
            "group by c.cmno, c.name, c.category order by total desc")
    List<Object[]> sumAmountByCompanyForPeriod(@Param("category") CompanyCategory category,
                                                @Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end);

    // 관리자 대시보드 "업체 월별 매출 추이" (전체) - 결제완료 예약의 결제일시/금액 원본 (월별 집계는 서비스단에서)
    @Query("select r.paidAt, r.amount from Reservation r where r.payStatus = 'PAID' and r.paidAt >= :since")
    List<Object[]> findPaidAmountRowsSince(@Param("since") LocalDateTime since);

    // 관리자 대시보드 "업체 월별 매출 추이" (카테고리별 드롭다운용)
    @Query("select r.paidAt, r.amount from Reservation r join Company c on r.cmno = c.cmno " +
            "where r.payStatus = 'PAID' and c.category = :category and r.paidAt >= :since")
    List<Object[]> findPaidAmountRowsByCategorySince(@Param("category") CompanyCategory category,
                                                      @Param("since") LocalDateTime since);

}
