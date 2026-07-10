package com.wedding.ddayevent.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wedding.ddayevent.dto.DdayEventDTO;
import com.wedding.ddayevent.service.DdayEventService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/ddayevents")
public class DdayEventController {

    private final DdayEventService service;

    @GetMapping("/member/{memberEmail}")
    public List<DdayEventDTO> listByMember(@PathVariable(name = "memberEmail") String memberEmail) {

        return service.listByMember(memberEmail);
    }

    @PreAuthorize("hasAnyRole('USER')")
    @PostMapping("/")
    public Map<String, Long> register(@RequestBody DdayEventDTO ddayEventDTO, Principal principal) {

        ddayEventDTO.setMemberEmail(principal.getName());

        log.info("DdayEventDTO: " + ddayEventDTO);

        Long ddayId = service.register(ddayEventDTO);

        return Map.of("ddayId", ddayId);
    }

    @PreAuthorize("hasAnyRole('USER')")
    @PutMapping("/{ddayId}")
    public Map<String, String> modify(
            @PathVariable(name = "ddayId") Long ddayId,
            @RequestBody DdayEventDTO ddayEventDTO,
            Principal principal) {

        ddayEventDTO.setDdayId(ddayId);

        log.info("Modify: " + ddayEventDTO);

        service.modify(ddayEventDTO, principal.getName());

        return Map.of("RESULT", "SUCCESS");
    }

    @PreAuthorize("hasAnyRole('USER')")
    @DeleteMapping("/{ddayId}")
    public Map<String, String> remove(
            @PathVariable(name = "ddayId") Long ddayId,
            Principal principal) {

        log.info("Remove: " + ddayId);

        service.remove(ddayId, principal.getName());

        return Map.of("RESULT", "SUCCESS");
    }

}
