package com.wedding.inquiry.domain;

import com.wedding.global.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "tbl_inquiry_room",
        uniqueConstraints = {
                // 같은 회원이 같은 업체에 중복으로 문의방을 만들지 않음
                @UniqueConstraint(columnNames = {"member_email", "cmno"})
        }
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InquiryRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long roomId;

    // 문의한 회원 이메일
    @Column(name = "member_email", nullable = false)
    private String memberEmail;

    // 업체 번호
    @Column(nullable = false)
    private Long cmno;

    // 마지막 메시지 시각 (매니저 목록에서 최신순 정렬)
    private LocalDateTime lastMessageAt;

    // OPEN: 진행 중 / CLOSED: 종료
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private InquiryRoomStatus status = InquiryRoomStatus.OPEN;

    // 회원이 메시지를 마지막으로 확인한 시각
    private LocalDateTime memberLastReadAt;

    // 매니저가 메시지를 마지막으로 확인한 시각
    private LocalDateTime managerLastReadAt;

    // 메시지 전송 시 LastMessageAt 갱신
    public void updateLastMessageAt(LocalDateTime time) {
        this.lastMessageAt = time;
    }

    public void close() {
        this.status = InquiryRoomStatus.CLOSED;
    }

    // 회원이 메시지를 읽은 시각 갱신
    public void markReadByMember(LocalDateTime time) {
        this.memberLastReadAt = time;
    }

    // 매니저가 메시지를 읽은 시각 갱신
    public void markReadByManager(LocalDateTime time) {
        this.managerLastReadAt = time;
    }

}
