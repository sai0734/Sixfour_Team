package com.wedding.member.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wedding.member.dto.JoinDTO;
import com.wedding.member.service.MemberService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/api/auth")
public class JoinController {

  private final MemberService memberService;

  @PostMapping("/join")
  public Map<String, String> join(@RequestBody JoinDTO joinDTO) {

    log.info("join: " + joinDTO);

    memberService.join(joinDTO);

    return Map.of("result", "success");
  }

}