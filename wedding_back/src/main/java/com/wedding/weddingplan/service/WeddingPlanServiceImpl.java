package com.wedding.weddingplan.service;

import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import com.wedding.weddingplan.domain.WeddingPlan;
import com.wedding.weddingplan.dto.WeddingPlanDTO;
import com.wedding.weddingplan.repository.WeddingPlanRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class WeddingPlanServiceImpl implements WeddingPlanService {

    private final WeddingPlanRepository weddingPlanRepository;

    private final ModelMapper modelMapper;

    @Override
    public Long register(WeddingPlanDTO weddingPlanDTO) {

        log.info("weddingPlan register.........");

        // 1:1 관계라 이미 등록된 회원이면 중복 생성 방지
        Optional<WeddingPlan> existing = weddingPlanRepository.findByMemberEmail(weddingPlanDTO.getMemberEmail());

        if (existing.isPresent()) {
            throw new IllegalStateException("이미 등록된 웨딩플랜이 존재합니다. memberEmail: " + weddingPlanDTO.getMemberEmail());
        }

        WeddingPlan weddingPlan = modelMapper.map(weddingPlanDTO, WeddingPlan.class);

        WeddingPlan savedWeddingPlan = weddingPlanRepository.save(weddingPlan);

        return savedWeddingPlan.getWeddingPlanId();
    }

    @Override
    public WeddingPlanDTO getByMember(String memberEmail) {

        Optional<WeddingPlan> result = weddingPlanRepository.findByMemberEmail(memberEmail);

        WeddingPlan weddingPlan = result.orElseThrow();

        return modelMapper.map(weddingPlan, WeddingPlanDTO.class);
    }

    @Override
    public void modify(WeddingPlanDTO weddingPlanDTO, String requesterEmail) {

        Optional<WeddingPlan> result = weddingPlanRepository.findById(weddingPlanDTO.getWeddingPlanId());

        WeddingPlan weddingPlan = result.orElseThrow();

        if (!weddingPlan.getMemberEmail().equals(requesterEmail)) {
            throw new IllegalStateException("본인의 웨딩플랜만 수정할 수 있습니다.");
        }

        weddingPlan.changeGroomName(weddingPlanDTO.getGroomName());
        weddingPlan.changeBrideName(weddingPlanDTO.getBrideName());
        weddingPlan.changeWeddingDate(weddingPlanDTO.getWeddingDate());
        weddingPlan.changeTotalBudget(weddingPlanDTO.getTotalBudget());
        weddingPlan.changeMemo(weddingPlanDTO.getMemo());

        weddingPlanRepository.save(weddingPlan);
    }

    @Override
    public void remove(Long weddingPlanId, String requesterEmail) {

        Optional<WeddingPlan> result = weddingPlanRepository.findById(weddingPlanId);

        WeddingPlan weddingPlan = result.orElseThrow();

        if (!weddingPlan.getMemberEmail().equals(requesterEmail)) {
            throw new IllegalStateException("본인의 웨딩플랜만 삭제할 수 있습니다.");
        }

        weddingPlanRepository.deleteById(weddingPlanId);
    }

}
