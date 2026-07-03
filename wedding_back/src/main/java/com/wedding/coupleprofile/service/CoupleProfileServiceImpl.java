package com.wedding.coupleprofile.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import com.wedding.coupleprofile.domain.CoupleProfile;
import com.wedding.coupleprofile.dto.CoupleProfileDTO;
import com.wedding.coupleprofile.repository.CoupleProfileRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class CoupleProfileServiceImpl implements CoupleProfileService {

    private final CoupleProfileRepository coupleProfileRepository;

    private final ModelMapper modelMapper;

    @Override
    public Long register(CoupleProfileDTO coupleProfileDTO) {

        log.info("coupleProfile register.........");

        Optional<CoupleProfile> existing =
                coupleProfileRepository.findByMemberEmail(coupleProfileDTO.getMemberEmail());

        if (existing.isPresent()) {
            throw new IllegalStateException(
                    "이미 등록된 프로필이 존재합니다. memberEmail: " + coupleProfileDTO.getMemberEmail());
        }

        CoupleProfile coupleProfile = modelMapper.map(coupleProfileDTO, CoupleProfile.class);

        CoupleProfile saved = coupleProfileRepository.save(coupleProfile);

        return saved.getProfileId();
    }

    @Override
    public CoupleProfileDTO getByMember(String memberEmail) {

        Optional<CoupleProfile> result = coupleProfileRepository.findByMemberEmail(memberEmail);

        CoupleProfile coupleProfile = result.orElseThrow();

        return modelMapper.map(coupleProfile, CoupleProfileDTO.class);
    }

    @Override
    public void modify(CoupleProfileDTO coupleProfileDTO) {

        Optional<CoupleProfile> result =
                coupleProfileRepository.findById(coupleProfileDTO.getProfileId());

        CoupleProfile coupleProfile = result.orElseThrow();

        coupleProfile.changeBudgetMin(coupleProfileDTO.getBudgetMin());
        coupleProfile.changeBudgetMax(coupleProfileDTO.getBudgetMax());
        coupleProfile.changeRegion(coupleProfileDTO.getRegion());
        coupleProfile.changeWeddingStyle(coupleProfileDTO.getWeddingStyle());
        coupleProfile.changeWeddingDate(coupleProfileDTO.getWeddingDate());
        coupleProfile.changeBio(coupleProfileDTO.getBio());

        coupleProfileRepository.save(coupleProfile);
    }

    @Override
    public void remove(Long profileId) {

        coupleProfileRepository.deleteById(profileId);
    }

    @Override
    public List<CoupleProfileDTO> listOthers(String excludeMemberEmail) {

        List<CoupleProfile> result = coupleProfileRepository.findAllByOrderByProfileIdDesc();

        return result.stream()
                .filter(cp -> !cp.getMemberEmail().equals(excludeMemberEmail))
                .map(cp -> modelMapper.map(cp, CoupleProfileDTO.class))
                .collect(Collectors.toList());
    }

}
