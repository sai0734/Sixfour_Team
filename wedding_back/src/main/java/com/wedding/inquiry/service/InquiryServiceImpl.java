package com.wedding.inquiry.service;

import com.wedding.inquiry.domain.InquiryMessage;
import com.wedding.inquiry.domain.InquiryRoom;
import com.wedding.inquiry.dto.InquiryMessageDTO;
import com.wedding.inquiry.dto.InquiryRoomDTO;
import com.wedding.inquiry.repository.InquiryMessageRepository;
import com.wedding.inquiry.repository.InquiryRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Log4j2
public class InquiryServiceImpl implements InquiryService {

    private final InquiryRoomRepository inquiryRoomRepository;
    private final InquiryMessageRepository inquiryMessageRepository;
    private final InquiryAccessService inquiryAccessService;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String MANAGER_VIEWER_MARKER = "__MANAGER_VIEW__";

    // 문의하기 클릭 시 — 기존 방 있으면 반환, 없으면 새로 생성
    @Override
    public InquiryRoomDTO getOrCreateRoom(String memberEmail, Long cmno) {
        log.info("InquiryServiceImpl_getOrCreateRoom_실행~~~~~~~~~~~");

        inquiryAccessService.requireCanOpenRoom(memberEmail, cmno);

        return inquiryRoomRepository.findByMemberEmailAndCmno(memberEmail, cmno)
                .map(room -> InquiryRoomDTO.from(room, memberEmail))
                .orElseGet(() -> {
                    InquiryRoom newRoom = InquiryRoom.builder()
                            .memberEmail(memberEmail)
                            .cmno(cmno)
                            .build();
                    InquiryRoom saved = inquiryRoomRepository.save(newRoom);
                    return InquiryRoomDTO.from(saved, memberEmail);
                });
    }

    // 매니저 화면 - 해당 업체(cmno)의 모든 문의방 목록 (최근 대화순)
    @Override
    @Transactional(readOnly = true)
    public List<InquiryRoomDTO> listRoomsByCompany(Long cmno, String callerEmail) {
        log.info("InquiryServiceImpl_listRoomsByCompany_실행~~~~~~~~~~~");

        inquiryAccessService.requireCanListCompanyRooms(callerEmail, cmno);

        return inquiryRoomRepository.findByCmnoOrderByLastMessageAtDesc(cmno)
                .stream()
                .map(room -> InquiryRoomDTO.from(room, callerEmail))
                .toList();
    }

    // 회원 화면 - 내가 연 모든 문의방 목록 (안읽음 뱃지 폴링용)
    @Override
    @Transactional(readOnly = true)
    public List<InquiryRoomDTO> listRoomsByMember(String callerEmail) {
        log.info("InquiryServiceImpl_listRoomsByMember_실행~~~~~~~~~~~");

        return inquiryRoomRepository.findByMemberEmailOrderByLastMessageAtDesc(callerEmail)
                .stream()
                .map(room -> InquiryRoomDTO.from(room, callerEmail))
                .toList();
    }

    // 채팅창 열 때 / 풀링할 때 특정 방의 메시지 목록 (시간순)
    @Override
    // 조회와 동시에 읽음 시각을 갱신해야 해서 readOnly 트랜잭션에서 제외
    public List<InquiryMessageDTO> getMessages(Long roomId, String callerEmail) {
        log.info("InquiryServiceImpl_getMessages_실행~~~~~~~~~~~");

        InquiryRoom room = inquiryAccessService.requireAccessibleRoom(callerEmail, roomId);

        LocalDateTime now = LocalDateTime.now();
        if (callerEmail.equals(room.getMemberEmail())) {
            room.markReadByMember(now);
            InquiryRoomDTO memberView = InquiryRoomDTO.from(room, room.getMemberEmail());
            messagingTemplate.convertAndSend("/topic/inquiries/member/" + room.getMemberEmail(), memberView);
        } else {
            room.markReadByManager(now);
            InquiryRoomDTO managerView = InquiryRoomDTO.from(room, MANAGER_VIEWER_MARKER);
            messagingTemplate.convertAndSend("/topic/inquiries/company/" + room.getCmno(), managerView);
        }

        return inquiryMessageRepository.findByRoomIdOrderByRegDateAsc(roomId)
                .stream()
                .map(InquiryMessageDTO::from)
                .toList();
    }

    // 메시지 전송 - 저장 + 방의 LastMessageAt 갱신
    @Override
    public InquiryMessageDTO sendMessage(Long roomId, String senderEmail, String content) {
        log.info("InquiryServiceImpl_sendMessage_실행~~~~~~~~~~~");

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("메시지 내용을 입력해주세요.");
        }

        InquiryRoom room = inquiryAccessService.requireAccessibleRoom(senderEmail, roomId);

        LocalDateTime now = LocalDateTime.now();
        InquiryMessage message = InquiryMessage.builder()
                .roomId(roomId)
                .senderEmail(senderEmail)
                .content(content.trim())
                .build();
        InquiryMessage saved = inquiryMessageRepository.save(message);

        room.updateLastMessageAt(now);

        // 보낸 사람 본인은 당연히 자기가 방금 보낸 메시지까지는 읽은 상태이므로,
        // 그 사람의 읽음 시각도 같이 갱신 (안 하면 본인이 보낸 메시지로 자기 자신에게 안읽음이 뜸)
        if (senderEmail.equals(room.getMemberEmail())) {
            room.markReadByMember(now);
        } else {
            room.markReadByManager(now);
        }

        InquiryMessageDTO messageDTO = InquiryMessageDTO.from(saved);

        // 채팅창이 열려있는 쪽(방을 구독 중인 클라이언트)에게 새 메시지를 즉시 밀어줌
        messagingTemplate.convertAndSend("/topic/inquiries/room/" + roomId, messageDTO);

        // 회원/매니저 양쪽 뱃지·목록용 토픽에도 갱신된 방 상태를 밀어줌 (누가 보냈든 둘 다 lastMessageAt이 바뀌었으므로)
        InquiryRoomDTO memberView = InquiryRoomDTO.from(room, room.getMemberEmail());
        messagingTemplate.convertAndSend("/topic/inquiries/member/" + room.getMemberEmail(), memberView);

        InquiryRoomDTO managerView = InquiryRoomDTO.from(room, MANAGER_VIEWER_MARKER);
        messagingTemplate.convertAndSend("/topic/inquiries/company/" + room.getCmno(), managerView);

        return messageDTO;

    }
}