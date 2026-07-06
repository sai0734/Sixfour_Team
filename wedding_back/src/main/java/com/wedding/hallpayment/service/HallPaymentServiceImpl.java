package com.wedding.hallpayment.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import com.wedding.hallpayment.domain.HallPayment;
import com.wedding.hallpayment.dto.HallPaymentDTO;
import com.wedding.hallpayment.repository.HallPaymentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class HallPaymentServiceImpl implements HallPaymentService {

    private final HallPaymentRepository hallPaymentRepository;

    private final ModelMapper modelMapper;

    @Override
    public Long register(HallPaymentDTO hallPaymentDTO) {

        log.info("hallPayment register.........");

        HallPayment hallPayment = modelMapper.map(hallPaymentDTO, HallPayment.class);

        HallPayment saved = hallPaymentRepository.save(hallPayment);

        return saved.getPaymentId();
    }

    @Override
    public HallPaymentDTO get(Long paymentId) {

        Optional<HallPayment> result = hallPaymentRepository.findById(paymentId);

        HallPayment hallPayment = result.orElseThrow();

        return modelMapper.map(hallPayment, HallPaymentDTO.class);
    }

    @Override
    public void modify(HallPaymentDTO hallPaymentDTO) {

        Optional<HallPayment> result = hallPaymentRepository.findById(hallPaymentDTO.getPaymentId());

        HallPayment hallPayment = result.orElseThrow();

        hallPayment.changePaymentType(hallPaymentDTO.getPaymentType());
        hallPayment.changeAmount(hallPaymentDTO.getAmount());
        hallPayment.changeStatus(hallPaymentDTO.getStatus());
        hallPayment.changeDueDate(hallPaymentDTO.getDueDate());
        hallPayment.changeImpUid(hallPaymentDTO.getImpUid());

        hallPaymentRepository.save(hallPayment);
    }

    @Override
    public void remove(Long paymentId) {

        hallPaymentRepository.deleteById(paymentId);
    }

    @Override
    public List<HallPaymentDTO> listByMember(String memberEmail) {

        List<HallPayment> result = hallPaymentRepository.findByMemberEmailOrderByDueDateAsc(memberEmail);

        return result.stream()
                .map(hp -> modelMapper.map(hp, HallPaymentDTO.class))
                .collect(Collectors.toList());
    }

}
