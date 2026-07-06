package com.wedding.hallpayment.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wedding.hallpayment.dto.HallPaymentDTO;
import com.wedding.hallpayment.service.HallPaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/hallpayments")
public class HallPaymentController {

    private final HallPaymentService service;

    @GetMapping("/{paymentId}")
    public HallPaymentDTO get(@PathVariable(name = "paymentId") Long paymentId) {

        return service.get(paymentId);
    }

    // 마이페이지 "결제 내역" + 준비관리 "납부 관리" 공용
    @GetMapping("/member/{memberEmail}")
    public List<HallPaymentDTO> listByMember(@PathVariable(name = "memberEmail") String memberEmail) {

        return service.listByMember(memberEmail);
    }

    @PostMapping("/")
    public Map<String, Long> register(@RequestBody HallPaymentDTO hallPaymentDTO) {

        log.info("HallPaymentDTO: " + hallPaymentDTO);

        Long paymentId = service.register(hallPaymentDTO);

        return Map.of("paymentId", paymentId);
    }

    @PutMapping("/{paymentId}")
    public Map<String, String> modify(
            @PathVariable(name = "paymentId") Long paymentId,
            @RequestBody HallPaymentDTO hallPaymentDTO) {

        hallPaymentDTO.setPaymentId(paymentId);

        log.info("Modify: " + hallPaymentDTO);

        service.modify(hallPaymentDTO);

        return Map.of("RESULT", "SUCCESS");
    }

    @DeleteMapping("/{paymentId}")
    public Map<String, String> remove(@PathVariable(name = "paymentId") Long paymentId) {

        log.info("Remove: " + paymentId);

        service.remove(paymentId);

        return Map.of("RESULT", "SUCCESS");
    }

}
