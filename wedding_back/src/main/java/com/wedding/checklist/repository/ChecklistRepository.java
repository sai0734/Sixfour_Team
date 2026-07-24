package com.wedding.checklist.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wedding.checklist.domain.Checklist;

public interface ChecklistRepository extends JpaRepository<Checklist, Long> {

    // 특정 회원의 체크리스트 전체를 단계 -> sortOrder 순으로 조회 (화면에서 단계별 그룹핑용)
    List<Checklist> findByMemberEmailOrderByStageAscSortOrderAsc(String memberEmail);

    // 그 예약에서 이미 자동 생성한 체크리스트 항목이 있는지 (결제대기 중복 처리 방지)
    boolean existsByReservationId(Long reservationId);

    // 결제 완료 시 그 예약이 만든 체크리스트 항목을 찾아 완료 체크하기 위함
    // (ReservationServiceImpl.syncPaymentArtifacts)
    Optional<Checklist> findByReservationId(Long reservationId);

}
