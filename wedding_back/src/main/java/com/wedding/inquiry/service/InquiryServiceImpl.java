package com.wedding.inquiry.service;

import com.wedding.inquiry.domain.InquiryMessage;
import com.wedding.inquiry.domain.InquiryRoom;
import com.wedding.inquiry.dto.InquiryMessageDTO;
import com.wedding.inquiry.dto.InquiryRoomDTO;
import com.wedding.inquiry.repository.InquiryMessageRepository;
import com.wedding.inquiry.repository.InquiryRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Log4j2
public class InquiryServiceImpl implements InquiryService{

    private final InquiryRoomRepository inquiryRoomRepository;
    private final InquiryMessageRepository inquiryMessageRepository;

    // 문의하기 클릭 시 — 기존 방 있으면 반환, 없으면 새로 생성
    @Override
    public InquiryRoomDTO getOrCreateRoom(String memberEmail, Long cmno) {

        log.info("InquiryServiceImpl_getOrCreateRoom_실행~~~~~~~~~~~");

        return inquiryRoomRepository.findByMemberEmailAndCmno(memberEmail, cmno)
                .map(room -> InquiryRoomDTO.from(room))
                .orElseGet(() -> {
                    InquiryRoom newRoom = InquiryRoom.builder()
                            .memberEmail(memberEmail)
                            .cmno(cmno)
                            .build();
                    InquiryRoom saved = inquiryRoomRepository.save(newRoom);
                    return InquiryRoomDTO.from(saved);
                });
    }

    // 매니저 화면 - 해당 업체(cmno)의 모든 문의방 목록 (최근 대화순)
    @Override
    public List<InquiryRoomDTO> listRoomsByCompany(Long cmno) {

        log.info("InquiryServiceImpl_listRoomsByCompany_실행~~~~~~~~~~~");

        return inquiryRoomRepository.findByCmnoOrderByLastMessageAtDesc(cmno)
                .stream().map(room -> InquiryRoomDTO.from(room))
                .toList();

    }

    // 채팅창 열 때 / 풀링할 때 특정 방의 메시지 목록 (시간순)
    @Override
    public List<InquiryMessageDTO> getMessages(Long roomId) {

        log.info("InquiryServiceImpl_getMessages_실행~~~~~~~~~~~");

        // 방 존재 여부 확인 (없는 roomId면 예외)
        inquiryRoomRepository.findById(roomId).orElseThrow();
        return inquiryMessageRepository.findByRoomIdOrderByRegDateAsc(roomId)
                .stream()
                .map(message -> InquiryMessageDTO.from(message))
                .toList();
    }

    // 메시지 전송 - 저장 + 방의 LastMessageAt 갱신
    @Override
    @Transactional
    public InquiryMessageDTO sendMessage(Long roomId, String senderEmail, String content) {

        log.info("InquiryServiceImpl_sendMessage_실행~~~~~~~~~~~");

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("메시지 내용을 입력해주세요.");
        }
        InquiryRoom room = inquiryRoomRepository.findById(roomId).orElseThrow();
        LocalDateTime now = LocalDateTime.now();
        InquiryMessage message = InquiryMessage.builder()
                .roomId(roomId)
                .senderEmail(senderEmail)
                .content(content.trim())
                .build();
        InquiryMessage saved = inquiryMessageRepository.save(message);
        room.updateLastMessageAt(now);
        return InquiryMessageDTO.from(saved);
    }

}
