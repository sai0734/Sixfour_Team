package com.wedding.inquiry.service;

import com.wedding.inquiry.domain.InquiryMessage;
import com.wedding.inquiry.domain.InquiryRoom;
import com.wedding.inquiry.dto.InquiryMessageDTO;
import com.wedding.inquiry.dto.InquiryRoomDTO;
import com.wedding.inquiry.dto.InquiryRoomEventDTO;
import com.wedding.inquiry.repository.InquiryMessageRepository;
import com.wedding.inquiry.repository.InquiryRoomRepository;
import com.wedding.member.domain.Member;
import com.wedding.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
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
    private final MemberRepository memberRepository;

    private static final String MANAGER_VIEWER_MARKER = "__MANAGER_VIEW__";
    // 채팅창을 열 때 한 번에 불러오는 최근 메시지 개수 — 대화가 길어져도 매번 전체를 다시 안 불러온다
    private static final int MESSAGE_LOAD_LIMIT = 50;

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
                .map(room -> InquiryRoomDTO.from(room, callerEmail, resolveMemberNickname(room.getMemberEmail())))
                .toList();
    }

    // 문의한 회원의 닉네임 조회 — 매니저 화면(목록·채팅헤더)에서 이메일 대신 표시하기 위함
    private String resolveMemberNickname(String memberEmail) {
        return memberRepository.findById(memberEmail).map(Member::getNickname).orElse(null);
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

    // 채팅창 열 때 / 풀링할 때 특정 방의 메시지 목록 (시간순, 최근 MESSAGE_LOAD_LIMIT개까지만)
    @Override
    // 조회와 동시에 읽음 시각을 갱신해야 해서 readOnly 트랜잭션에서 제외
    public List<InquiryMessageDTO> getMessages(Long roomId, String callerEmail) {
        log.info("InquiryServiceImpl_getMessages_실행~~~~~~~~~~~");

        InquiryRoom room = inquiryAccessService.requireAccessibleRoom(callerEmail, roomId);

        // 최신순으로 최대 N개만 가져온 뒤, 화면 표시 순서(오래된 것 → 최신)로 뒤집는다
        List<InquiryMessage> recent = new ArrayList<>(
                inquiryMessageRepository.findByRoomIdOrderByRegDateDesc(
                        roomId, PageRequest.of(0, MESSAGE_LOAD_LIMIT)));
        Collections.reverse(recent);

        markReadAndBroadcast(room, callerEmail);

        return recent.stream()
                .map(message -> InquiryMessageDTO.from(message, room))
                .toList();
    }

    // 채팅창을 열어둔 채로 실시간 메시지를 받았을 때 — 목록을 다시 안 불러오고 읽음 시각만 갱신
    @Override
    public void markRead(Long roomId, String callerEmail) {
        log.info("InquiryServiceImpl_markRead_실행~~~~~~~~~~~");

        InquiryRoom room = inquiryAccessService.requireAccessibleRoom(callerEmail, roomId);
        markReadAndBroadcast(room, callerEmail);
    }

    // 읽음 시각 갱신 + 회원/매니저 뱃지 토픽 + 상대방이 지금 보고 있을 채팅창(room 토픽)까지 전부 알린다
    private void markReadAndBroadcast(InquiryRoom room, String callerEmail) {
        LocalDateTime now = LocalDateTime.now();

        if (callerEmail.equals(room.getMemberEmail())) {
            room.markReadByMember(now);
            InquiryRoomDTO memberView = InquiryRoomDTO.from(room, room.getMemberEmail());
            messagingTemplate.convertAndSend("/topic/inquiries/member/" + room.getMemberEmail(), memberView);
        } else {
            room.markReadByManager(now);
            InquiryRoomDTO managerView = InquiryRoomDTO.from(
                    room, MANAGER_VIEWER_MARKER, resolveMemberNickname(room.getMemberEmail()));
            messagingTemplate.convertAndSend("/topic/inquiries/company/" + room.getCmno(), managerView);
        }

        messagingTemplate.convertAndSend(
                "/topic/inquiries/room/" + room.getRoomId(),
                InquiryRoomEventDTO.read(callerEmail, now));
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

        InquiryMessageDTO messageDTO = InquiryMessageDTO.from(saved, room);

        // 채팅창이 열려있는 쪽(방을 구독 중인 클라이언트)에게 새 메시지를 즉시 밀어줌
        messagingTemplate.convertAndSend(
                "/topic/inquiries/room/" + roomId, InquiryRoomEventDTO.message(messageDTO));

        // 회원/매니저 양쪽 뱃지·목록용 토픽에도 갱신된 방 상태를 밀어줌 (누가 보냈든 둘 다 lastMessageAt이 바뀌었으므로)
        InquiryRoomDTO memberView = InquiryRoomDTO.from(room, room.getMemberEmail());
        messagingTemplate.convertAndSend("/topic/inquiries/member/" + room.getMemberEmail(), memberView);

        InquiryRoomDTO managerView = InquiryRoomDTO.from(
                room, MANAGER_VIEWER_MARKER, resolveMemberNickname(room.getMemberEmail()));
        messagingTemplate.convertAndSend("/topic/inquiries/company/" + room.getCmno(), managerView);

        return messageDTO;
    }
}
