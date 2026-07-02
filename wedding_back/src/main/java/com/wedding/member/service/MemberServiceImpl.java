package com.wedding.member.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.wedding.global.advice.DuplicateEmailException;
import com.wedding.global.util.MailService;
import com.wedding.member.domain.EmailVerify;
import com.wedding.member.domain.Member;
import com.wedding.member.domain.MemberDetail;
import com.wedding.member.domain.MemberRole;
import com.wedding.member.domain.TermsAgree;
import com.wedding.member.dto.JoinDTO;
import com.wedding.member.dto.MemberDTO;
import com.wedding.member.dto.MemberModifyDTO;
import com.wedding.member.dto.PasswordResetConfirmDTO;
import com.wedding.member.dto.PasswordResetRequestDTO;
import com.wedding.member.dto.SocialCompleteDTO;
import com.wedding.member.repository.EmailVerifyRepository;
import com.wedding.member.repository.LoginFailRepository;
import com.wedding.member.repository.MemberDetailRepository;
import com.wedding.member.repository.MemberRepository;
import com.wedding.member.repository.PasswordResetRepository;
import com.wedding.member.repository.SocialAccountRepository;
import com.wedding.member.repository.TermsAgreeRepository;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedding.member.domain.PasswordReset;
import com.wedding.member.domain.SocialAccount;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class MemberServiceImpl implements MemberService {

  private final MemberRepository memberRepository;
  private final MemberDetailRepository memberDetailRepository;
  private final TermsAgreeRepository termsAgreeRepository;
  private final EmailVerifyRepository emailVerifyRepository;
  private final PasswordResetRepository passwordResetRepository;
  private final LoginFailRepository loginFailRepository;
  private final SocialAccountRepository socialAccountRepository;
  private final MailService mailService;
  private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;

  @Override
  public MemberDTO getKakaoMember(String accessToken) {

    Map<String, String> kakaoInfo = getKakaoUserInfo(accessToken);
    String email = kakaoInfo.get("email");
    String providerId = kakaoInfo.get("providerId");

    log.info("email: " + email );

Optional<Member> result = memberRepository.findById(email);

    // 기존의 회원
    if(result.isPresent()){
      ensureSocialAccount(result.get(), providerId);

      MemberDTO memberDTO = entityToDTO(result.get());

      return memberDTO;
    }

    // 회원이 아니었다면
    // 닉네임은 '소셜회원'으로
    // 패스워드는 임의로 생성
    Member socialMember = makeSocialMember(email);
    memberRepository.save(socialMember);

    ensureSocialAccount(socialMember, providerId);

    MemberDTO memberDTO = entityToDTO(socialMember);

    return memberDTO;
  }

  private void ensureSocialAccount(Member member, String providerId) {

    if (socialAccountRepository.findByProviderAndProviderId("kakao", providerId).isPresent()) {
      return;
    }

    SocialAccount socialAccount = SocialAccount.builder()
            .member(member)
            .provider("kakao")
            .providerId(providerId)
            .build();

    socialAccountRepository.save(socialAccount);
  }

  private Map<String, String> getKakaoUserInfo(String accessToken){

    String kakaoGetUserURL = "https://kapi.kakao.com/v2/user/me";

    if(accessToken == null){
      throw new RuntimeException("Access Token is null");
    }
    RestTemplate restTemplate = new RestTemplate();

    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Bearer " + accessToken);
    headers.add("Content-Type","application/x-www-form-urlencoded");
    HttpEntity<String> entity = new HttpEntity<>(headers);

    UriComponents uriBuilder = UriComponentsBuilder.fromHttpUrl(kakaoGetUserURL).build();

    ResponseEntity<LinkedHashMap> response = 
      restTemplate.exchange(
      uriBuilder.toString(), 
      HttpMethod.GET, 
      entity, 
      LinkedHashMap.class);

    log.info(response);

    LinkedHashMap<String, Object> bodyMap = response.getBody();

    log.info("------------------------------");
    log.info(bodyMap);

    LinkedHashMap<String, String> kakaoAccount = (LinkedHashMap<String, String>) bodyMap.get("kakao_account");

    log.info("kakaoAccount: " + kakaoAccount);

    String providerId = String.valueOf(bodyMap.get("id"));

    return Map.of("email", kakaoAccount.get("email"), "providerId", providerId);

  }

    private String makeTempPassword() {

    StringBuffer buffer = new StringBuffer();

    for(int i = 0;  i < 10; i++){
      buffer.append(  (char) ( (int)(Math.random()*55) + 65  ));
    }
    return buffer.toString();
  }

  
  private Member makeSocialMember(String email) {

   String tempPassword = makeTempPassword();

   log.info("tempPassword: " + tempPassword);

   // 닉네임 unique 제약이 있어서, 모두에게 "소셜회원"을 그대로 주면
   // 두 번째 카카오 가입자부터 저장이 실패함 → 임시로 겹치지 않는 값을 붙여줌
   // (추가정보 입력 화면에서 사용자가 원하는 닉네임으로 바로 바꾸게 됨)
   String nickname = "소셜회원" + java.util.UUID.randomUUID().toString().substring(0, 8);

   Member member = Member.builder()
   .email(email)
   .pw(passwordEncoder.encode(tempPassword))
   .nickname(nickname)
   .social(true)
   .build();

   member.addRole(MemberRole.USER);

   return member;

  }

  
    @Override
  public void modifyMember(MemberModifyDTO memberModifyDTO) {

    Optional<Member> result = memberRepository.findById(memberModifyDTO.getEmail());

    Member member = result.orElseThrow();

    member.changePw(passwordEncoder.encode(memberModifyDTO.getPw()));
    member.changeNickname(memberModifyDTO.getNickname());

    memberRepository.save(member);

  }

  @Override
  public boolean checkEmailAvailable(String email) {

    return memberRepository.findById(email).isEmpty();
  }

  @Override
  public boolean checkNicknameAvailable(String nickname) {

    return !memberRepository.existsByNickname(nickname);
  }

  @Override
  public void join(JoinDTO joinDTO) {

    // 이메일 중복 체크
    if (memberRepository.findById(joinDTO.getEmail()).isPresent()) {
      throw new DuplicateEmailException("이미 가입된 이메일입니다.");
    }

    // 비밀번호는 암호화해서 임시 저장 (평문 보관 방지)
    joinDTO.setPw(passwordEncoder.encode(joinDTO.getPw()));

    String token = UUID.randomUUID().toString();

    try {
      String payload = objectMapper.writeValueAsString(joinDTO);

      EmailVerify emailVerify = EmailVerify.builder()
              .email(joinDTO.getEmail())
              .token(token)
              .payload(payload)
              .expiredAt(LocalDateTime.now().plusMinutes(30))
              .build();

      emailVerifyRepository.save(emailVerify);

    } catch (Exception e) {
      log.error("가입 데이터 직렬화 실패: " + joinDTO.getEmail(), e);
      throw new RuntimeException("회원가입 처리 중 오류가 발생했습니다.");
    }

    try {
      mailService.sendVerificationEmail(joinDTO.getEmail(), token);
    } catch (Exception e) {
      // 메일 발송 실패해도 신청 자체는 접수된 상태로 둠 (인증 전이라 실제 계정은 아직 생성 안 됨)
      log.error("이메일 발송 실패: " + joinDTO.getEmail(), e);
    }

    log.info("join requested (pending verification): " + joinDTO.getEmail());
  }

  @Override
  public void completeSocialProfile(SocialCompleteDTO socialCompleteDTO) {

    Optional<Member> result = memberRepository.findById(socialCompleteDTO.getEmail());

    Member member = result.orElseThrow();

    // 이미 상세정보가 있는 회원이면 중복 처리 방지
    if (memberDetailRepository.getByMemberEmail(socialCompleteDTO.getEmail()).isPresent()) {
      throw new IllegalStateException("이미 추가정보 입력이 완료된 회원입니다.");
    }

    // 닉네임 중복 체크 (본인이 원래 가지고 있던 임시 닉네임과 동일하면 통과)
    if (!socialCompleteDTO.getNickname().equals(member.getNickname())
            && memberRepository.existsByNickname(socialCompleteDTO.getNickname())) {
      throw new IllegalStateException("이미 사용 중인 닉네임입니다.");
    }

    member.changeNickname(socialCompleteDTO.getNickname());
    memberRepository.save(member);

    // 1. MemberDetail 저장
    MemberDetail memberDetail = MemberDetail.builder()
            .member(member)
            .name(socialCompleteDTO.getName())
            .phone(socialCompleteDTO.getPhone())
            .birthDate(socialCompleteDTO.getBirthDate())
            .zipCode(socialCompleteDTO.getZipCode())
            .address(socialCompleteDTO.getAddress())
            .addressDetail(socialCompleteDTO.getAddressDetail())
            .build();

    memberDetailRepository.save(memberDetail);

    // 2. TermsAgree 저장
    TermsAgree termsAgree = TermsAgree.builder()
            .member(member)
            .termsAgree(socialCompleteDTO.isTermsAgree())
            .privacyAgree(socialCompleteDTO.isPrivacyAgree())
            .marketing(socialCompleteDTO.isMarketing())
            .build();

    termsAgreeRepository.save(termsAgree);

    log.info("social profile complete: " + socialCompleteDTO.getEmail());
  }

  @Override
  public boolean hasProfile(String email) {

    return memberDetailRepository.getByMemberEmail(email).isPresent();
  }

  @Override
  public String verifyEmail(String token) {

    Optional<EmailVerify> result = emailVerifyRepository.findByToken(token);

    if (result.isEmpty()) {
      return "INVALID_TOKEN";
    }

    EmailVerify emailVerify = result.get();

    if (emailVerify.isVerified()) {
      return "ALREADY_VERIFIED";
    }

    if (emailVerify.getExpiredAt().isBefore(LocalDateTime.now())) {
      return "EXPIRED";
    }

    // 그 사이에 이미 다른 경로로 가입이 완료된 경우 방어
    if (memberRepository.findById(emailVerify.getEmail()).isPresent()) {
      emailVerify.verify();
      emailVerifyRepository.save(emailVerify);
      return "ALREADY_VERIFIED";
    }

    try {
      JoinDTO joinDTO = objectMapper.readValue(emailVerify.getPayload(), JoinDTO.class);

      // 1. Member 저장 (인증을 통과한 시점이므로 emailVerified = true로 바로 생성)
      Member member = Member.builder()
              .email(joinDTO.getEmail())
              .pw(joinDTO.getPw()) // join()에서 이미 암호화되어 저장된 값
              .nickname(joinDTO.getNickname())
              .social(false)
              .emailVerified(true)
              .build();

      member.addRole(MemberRole.USER);

      memberRepository.save(member);

      // 2. MemberDetail 저장
      MemberDetail memberDetail = MemberDetail.builder()
              .member(member)
              .name(joinDTO.getName())
              .phone(joinDTO.getPhone())
              .birthDate(joinDTO.getBirthDate())
              .zipCode(joinDTO.getZipCode())
              .address(joinDTO.getAddress())
              .addressDetail(joinDTO.getAddressDetail())
              .build();

      memberDetailRepository.save(memberDetail);

      // 3. TermsAgree 저장
      TermsAgree termsAgree = TermsAgree.builder()
              .member(member)
              .termsAgree(joinDTO.isTermsAgree())
              .privacyAgree(joinDTO.isPrivacyAgree())
              .marketing(joinDTO.isMarketing())
              .build();

      termsAgreeRepository.save(termsAgree);

      emailVerify.verify();
      emailVerifyRepository.save(emailVerify);

      log.info("join success (verified): " + joinDTO.getEmail());

      return "SUCCESS";

    } catch (Exception e) {
      log.error("이메일 인증 처리 중 오류: " + emailVerify.getEmail(), e);
      return "INVALID_TOKEN";
    }
  }

  @Override
  public String resendVerification(String email) {

    if (memberRepository.findById(email).isPresent()) {
      return "ALREADY_VERIFIED";
    }

    Optional<EmailVerify> result = emailVerifyRepository.findTopByEmailOrderByIdDesc(email);

    if (result.isEmpty()) {
      return "NOT_FOUND";
    }

    EmailVerify previous = result.get();

    String newToken = UUID.randomUUID().toString();

    EmailVerify emailVerify = EmailVerify.builder()
            .email(email)
            .token(newToken)
            .payload(previous.getPayload())
            .expiredAt(LocalDateTime.now().plusMinutes(30))
            .build();

    emailVerifyRepository.save(emailVerify);

    try {
      mailService.sendVerificationEmail(email, newToken);
    } catch (Exception e) {
      log.error("이메일 재발송 실패: " + email, e);
      return "SEND_FAILED";
    }

    return "SUCCESS";
  }

  @Override
  public void requestPasswordReset(PasswordResetRequestDTO requestDTO) {

    Optional<Member> result = memberRepository.findById(requestDTO.getEmail());

    // 보안상, 가입 여부와 무관하게 항상 같은 응답을 주는 게 이상적이나
    // 우선은 명확한 에러로 처리 (존재하지 않으면 발송 자체를 안 함)
    if (result.isEmpty()) {
      log.info("password reset requested for non-existent email: " + requestDTO.getEmail());
      return;
    }

    Member member = result.get();

    String token = UUID.randomUUID().toString();

    PasswordReset passwordReset = PasswordReset.builder()
            .member(member)
            .token(token)
            .expiresAt(LocalDateTime.now().plusMinutes(30))
            .build();

    passwordResetRepository.save(passwordReset);

    try {
      mailService.sendPasswordResetEmail(member.getEmail(), token);
    } catch (Exception e) {
      log.error("비밀번호 재설정 메일 발송 실패: " + member.getEmail(), e);
    }
  }

  @Override
  public String confirmPasswordReset(PasswordResetConfirmDTO confirmDTO) {

    Optional<PasswordReset> result = passwordResetRepository.findByToken(confirmDTO.getToken());

    if (result.isEmpty()) {
      return "INVALID_TOKEN";
    }

    PasswordReset passwordReset = result.get();

    if (passwordReset.isUsed()) {
      return "ALREADY_USED";
    }

    if (passwordReset.getExpiresAt().isBefore(LocalDateTime.now())) {
      return "EXPIRED";
    }

    Member member = passwordReset.getMember();
    member.changePw(passwordEncoder.encode(confirmDTO.getNewPw()));
    memberRepository.save(member);

    // 비밀번호 재설정 = 본인 확인 완료로 간주하여 로그인 잠금도 함께 해제
    loginFailRepository.getByMemberEmail(member.getEmail())
            .ifPresent(loginFail -> {
              loginFail.reset();
              loginFailRepository.save(loginFail);
            });

    passwordReset.use();
    passwordResetRepository.save(passwordReset);

    log.info("password reset complete: " + member.getEmail());

    return "SUCCESS";
  }
}