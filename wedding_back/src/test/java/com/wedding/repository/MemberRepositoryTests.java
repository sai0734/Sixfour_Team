package com.wedding.repository;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.wedding.member.domain.Member;
import com.wedding.member.domain.MemberRole;
import com.wedding.member.repository.MemberRepository;



@SpringBootTest
@Log4j2
public class MemberRepositoryTests {

  @Autowired
  private MemberRepository memberRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Test
  public void testInsertMember(){

    // aaa@aaa.com ~ iii@iii.com (9개) : 일반 회원
    // jjj@jjj.com (1개) : 최고 관리자
    for (int i = 0; i < 10 ; i++) {

      char c = (char) ('a' + i);
      String triple = "" + c + c + c;
      String email = triple + "@" + triple + ".com";

      Member member = Member.builder()
              .email(email)
              .pw(passwordEncoder.encode("1111"))
              .nickname("USER_" + triple)
              .build();

      member.addRole(MemberRole.USER);

      if(i == 9){
          member.addRole(MemberRole.ADMIN);
      }
      memberRepository.save(member);
    }
  }

  @Test
  public void testRead() {

    String email = "jjj@jjj.com";

    Member member = memberRepository.getWithRoles(email);

    log.info("-----------------");
    log.info(member);
  }

}