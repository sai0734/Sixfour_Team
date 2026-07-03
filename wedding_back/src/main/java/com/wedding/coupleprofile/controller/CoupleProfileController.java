package com.wedding.coupleprofile.controller;

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

import com.wedding.coupleprofile.dto.CoupleProfileDTO;
import com.wedding.coupleprofile.service.CoupleProfileService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/coupleprofiles")
public class CoupleProfileController {

    private final CoupleProfileService service;

    @GetMapping("/member/{memberEmail}")
    public CoupleProfileDTO getByMember(@PathVariable(name = "memberEmail") String memberEmail) {

        return service.getByMember(memberEmail);
    }

    // 매칭 화면용 - 본인 제외 전체 목록
    // 예: GET /api/coupleprofiles/others/test@test.com
    @GetMapping("/others/{memberEmail}")
    public List<CoupleProfileDTO> listOthers(@PathVariable(name = "memberEmail") String memberEmail) {

        return service.listOthers(memberEmail);
    }

    @PostMapping("/")
    public Map<String, Long> register(@RequestBody CoupleProfileDTO coupleProfileDTO) {

        log.info("CoupleProfileDTO: " + coupleProfileDTO);

        Long profileId = service.register(coupleProfileDTO);

        return Map.of("profileId", profileId);
    }

    @PutMapping("/{profileId}")
    public Map<String, String> modify(
            @PathVariable(name = "profileId") Long profileId,
            @RequestBody CoupleProfileDTO coupleProfileDTO) {

        coupleProfileDTO.setProfileId(profileId);

        log.info("Modify: " + coupleProfileDTO);

        service.modify(coupleProfileDTO);

        return Map.of("RESULT", "SUCCESS");
    }

    @DeleteMapping("/{profileId}")
    public Map<String, String> remove(@PathVariable(name = "profileId") Long profileId) {

        log.info("Remove: " + profileId);

        service.remove(profileId);

        return Map.of("RESULT", "SUCCESS");
    }

}
