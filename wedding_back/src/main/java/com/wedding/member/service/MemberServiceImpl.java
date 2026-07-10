package com.wedding.member.service;

import java.util.LinkedHashMap;
import java.util.List;
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
import com.wedding.member.dto.KakaoAuthResultDTO;
import com.wedding.member.dto.KakaoLinkConfirmDTO;
import com.wedding.member.dto.KakaoSignupCompleteDTO;
import com.wedding.member.dto.MemberDTO;
import com.wedding.member.exception.MemberBlockedException;
import com.wedding.member.dto.MemberModifyDTO;
import com.wedding.member.dto.PasswordResetConfirmDTO;
import com.wedding.member.dto.PasswordResetRequestDTO;
import com.wedding.member.repository.EmailVerifyRepository;
import com.wedding.member.repository.LoginFailRepository;
import com.wedding.member.repository.MemberDetailRepository;
import com.wedding.member.repository.MemberRepository;
import com.wedding.member.repository.PasswordResetRepository;
import com.wedding.member.repository.SocialAccountRepository;
import com.wedding.member.repository.TermsAgreeRepository;
import com.wedding.global.util.JWTUtil;
import com.wedding.global.util.RedisTokenService;
import com.wedding.global.util.CustomJWTException;

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
  private final RedisTokenService redisTokenService;

  @Override
  public KakaoAuthResultDTO processKakaoAuth(String accessToken) {

    Map<String, String> kakaoInfo = getKakaoUserInfo(accessToken);
    String email = kakaoInfo.get("email");
    String providerId = kakaoInfo.get("providerId");

    log.info("email: " + email );

    // 1. 이미 연동(마이페이지 "연동하기")된 계정인지 providerId로 먼저 확인 -> 있으면 바로 로그인
    Optional<SocialAccount> linkedAccount = socialAccountRepository.findByProviderAndProviderId("kakao", providerId);

    if (linkedAccount.isPresent()) {
      Member member = linkedAccount.get().getMember();

      checkBlockedStatus(member);

      ensureSocialAccount(member, providerId);
      MemberDTO memberDTO = entityToDTO(member);
      return new KakaoAuthResultDTO("READY", memberDTO, null, null);
    }

    // 2. 연동 기록은 없지만, 카카오 계정 이메일과 같은 기존(가입완료) 회원이 있는지 확인.
    //    이 경우 조용히 자동 연동/로그인시키지 않고, 먼저 사용자에게 "이 계정과 연동할까요?"를 물어봐야 함
    Optional<Member> emailMatch = memberRepository.findById(email);

    if (emailMatch.isPresent()) {
      Member member = emailMatch.get();

      checkBlockedStatus(member);

      boolean hasProfile = memberDetailRepository.getByMemberEmail(member.getEmail()).isPresent();

      if (hasProfile) {
        Map<String, Object> confirmClaims = Map.of(
                "purpose", "KAKAO_EMAIL_MATCH_CONFIRM",
                "kakaoEmail", email,
                "providerId", providerId
        );

        String confirmToken = JWTUtil.generateToken(confirmClaims, 10); // 10분짜리 확인용 토큰

        return new KakaoAuthResultDTO("CONFIRM_LINK", null, confirmToken, email);
      }
      // 프로필이 아직 없으면(예전에 시작만 하고 안 끝낸 레거시 row) 아래 "미완료" 분기로 흘러감
    }

    // 3. 신규 또는 미완료 회원 -> 아직 로그인시키지 않음(Member도 새로 안 만듦).
    // 가입 마무리 화면(SocialCompletePage)에서 이 토큰을 들고 completeKakaoSignup을 호출해야
    // 그때 비로소 Member/MemberDetail이 만들어지고 진짜 로그인 토큰이 발급됨
    Map<String, Object> pendingClaims = Map.of(
            "purpose", "KAKAO_PENDING_SIGNUP",
            "kakaoEmail", email,
            "providerId", providerId
    );

    String pendingToken = JWTUtil.generateToken(pendingClaims, 30); // 30분짜리 임시 토큰

    return new KakaoAuthResultDTO("PENDING_SIGNUP", null, pendingToken, email);
  }

  // 정지/휴면/탈퇴 상태 체크 (정지기간 만료 시 자동 복귀 포함) - processKakaoAuth의 두 조회 분기에서 공통으로 씀
  private void checkBlockedStatus(Member member) {

    if ("BLACKLIST".equals(member.getStatus())
            && member.getSuspendUntil() != null
            && !member.getSuspendUntil().isAfter(LocalDateTime.now())) {
      member.reactivate();
      memberRepository.save(member);
    }

    if ("BLACKLIST".equals(member.getStatus())) {
      throw new MemberBlockedException(
              "ERROR_ACCOUNT_SUSPENDED",
              member.getSuspendReason(),
              member.getSuspendUntil() == null ? null : member.getSuspendUntil().toString());
    }

    if ("DORMANT".equals(member.getStatus())) {
      throw new MemberBlockedException("ERROR_ACCOUNT_DORMANT", null, null);
    }

    if ("WITHDRAWN".equals(member.getStatus())) {
      throw new MemberBlockedException("ERROR_ACCOUNT_WITHDRAWN", null, null);
    }
  }

  @Override
  public Map<String, Object> confirmKakaoEmailLink(KakaoLinkConfirmDTO dto) {

    Map<String, Object> confirmClaims;

    try {
      confirmClaims = JWTUtil.validateToken(dto.getConfirmToken());
    } catch (CustomJWTException e) {
      throw new IllegalStateException("확인 세션이 만료되었습니다. 카카오 로그인을 다시 시도해주세요.");
    }

    if (!"KAKAO_EMAIL_MATCH_CONFIRM".equals(confirmClaims.get("purpose"))) {
      throw new IllegalStateException("잘못된 요청입니다.");
    }

    String email = (String) confirmClaims.get("kakaoEmail");
    String providerId = (String) confirmClaims.get("providerId");

    Member member = memberRepository.findById(email).orElseThrow();

    // 그 사이(최대 10분) 다른 경로로 이미 연동됐을 수 있으니 방어적으로 재확인
    if (socialAccountRepository.findByProviderAndProviderId("kakao", providerId).isEmpty()) {
      ensureSocialAccount(member, providerId);
    }

    MemberDTO memberDTO = entityToDTO(member);
    Map<String, Object> resultClaims = memberDTO.getClaims();

    // 카카오 연동 로그인도 "로그인 유지" 개념이 없어 항상 7일(미유지)로 취급
    resultClaims.put("rememberMe", false);

    String jwtAccessToken = JWTUtil.generateToken(resultClaims, JWTUtil.ACCESS_TOKEN_MINUTES);
    String jwtRefreshToken = JWTUtil.generateToken(resultClaims, JWTUtil.REFRESH_TOKEN_MINUTES_DEFAULT);

    redisTokenService.saveRefreshToken(email, jwtRefreshToken, false);

    resultClaims.put("accessToken", jwtAccessToken);
    resultClaims.put("refreshToken", jwtRefreshToken);
    resultClaims.put("profileComplete", true);

    log.info("kakao email-match link confirmed: " + email);

    return resultClaims;
  }

  @Override
  public Map<String, Object> completeKakaoSignup(KakaoSignupCompleteDTO dto) {

    Map<String, Object> pendingClaims;

    try {
      pendingClaims = JWTUtil.validateToken(dto.getPendingToken());
    } catch (CustomJWTException e) {
      throw new IllegalStateException("가입 세션이 만료되었습니다. 카카오 로그인을 다시 시도해주세요.");
    }

    if (!"KAKAO_PENDING_SIGNUP".equals(pendingClaims.get("purpose"))) {
      throw new IllegalStateException("잘못된 요청입니다.");
    }

    String email = (String) pendingClaims.get("kakaoEmail");
    String providerId = (String) pendingClaims.get("providerId");

    // pendingToken 발급 이후(최대 30분 사이) 그 사이에 다른 경로로 이 카카오 계정이
    // 이미 연동/가입됐을 수 있으니 마지막에 한 번 더 확인 (경쟁 상황 방어)
    if (socialAccountRepository.findByProviderAndProviderId("kakao", providerId).isPresent()) {
      throw new IllegalStateException("이미 다른 계정에 연동된 카카오 계정입니다. 로그인해주세요.");
    }

    // 닉네임 중복 체크
    if (memberRepository.existsByNickname(dto.getNickname())) {
      throw new IllegalStateException("이미 사용 중인 닉네임입니다.");
    }

    // 전화번호 중복/차단 체크
    String phoneStatus = getPhoneCheckStatus(dto.getPhone());

    if ("BLOCKED".equals(phoneStatus)) {
      throw new IllegalStateException("정지 또는 휴면 처리된 회원과 동일한 전화번호입니다. 관리자에게 문의해주세요.");
    }

    if ("UNAVAILABLE".equals(phoneStatus)) {
      throw new IllegalStateException("이미 사용 중인 휴대폰 번호입니다.");
    }

    // 완전 신규면 새로 만들고, 예전에 시작만 하다 만 레거시 row가 있으면 그걸 그대로 채움
    Member member = memberRepository.findById(email).orElse(null);

    if (member == null) {
      member = Member.builder()
              .email(email)
              .pw(passwordEncoder.encode(makeTempPassword()))
              .nickname(dto.getNickname())
              .social(true)
              .build();
      member.addRole(MemberRole.USER);
    } else {
      member.changeNickname(dto.getNickname());
    }

    memberRepository.save(member);

    ensureSocialAccount(member, providerId);

    Optional<MemberDetail> existingDetail = memberDetailRepository.getByMemberEmail(email);

    MemberDetail memberDetail = existingDetail.isPresent()
            ? existingDetail.get()
            : MemberDetail.builder().member(member).build();

    memberDetail.changeName(dto.getName());
    memberDetail.changePhone(dto.getPhone());
    memberDetail.changeBirthDate(dto.getBirthDate());
    memberDetail.changeAddress(dto.getZipCode(), dto.getAddress(), dto.getAddressDetail());

    memberDetailRepository.save(memberDetail);

    TermsAgree termsAgree = TermsAgree.builder()
            .member(member)
            .termsAgree(dto.isTermsAgree())
            .privacyAgree(dto.isPrivacyAgree())
            .marketing(dto.isMarketing())
            .build();

    termsAgreeRepository.save(termsAgree);

    // 여기서 처음으로 진짜 로그인 처리 (JWT 발급)
    MemberDTO memberDTO = entityToDTO(member);
    Map<String, Object> resultClaims = memberDTO.getClaims();

    // 카카오 신규가입도 "로그인 유지" 개념이 없어 항상 7일(미유지)로 취급
    resultClaims.put("rememberMe", false);

    String jwtAccessToken = JWTUtil.generateToken(resultClaims, JWTUtil.ACCESS_TOKEN_MINUTES);
    String jwtRefreshToken = JWTUtil.generateToken(resultClaims, JWTUtil.REFRESH_TOKEN_MINUTES_DEFAULT);

    redisTokenService.saveRefreshToken(email, jwtRefreshToken, false);

    resultClaims.put("accessToken", jwtAccessToken);
    resultClaims.put("refreshToken", jwtRefreshToken);
    resultClaims.put("profileComplete", true);

    log.info("kakao signup complete: " + email);

    return resultClaims;
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

    // 비밀번호란을 안 건드렸으면(빈 값) 절대 덮어쓰지 않음
    // (예전엔 무조건 암호화해서 저장해버려서, 닉네임만 바꿔도 실제 비밀번호가
    //  빈 문자열로 조용히 날아가는 심각한 버그가 있었음)
    if (memberModifyDTO.getPw() != null && !memberModifyDTO.getPw().isBlank()) {
      member.changePw(passwordEncoder.encode(memberModifyDTO.getPw()));

      // 카카오로만 가입해서 social=true였던 회원이 여기서 처음 진짜 비밀번호를
      // 설정하면, 그 순간부터는 일반가입 회원과 동일하게 취급해야 함
      // (안 그러면 나중에 연동 해제 시 "먼저 비밀번호부터 설정하라"는 체크에
      //  영원히 걸림 - 비밀번호를 설정했는데도 여전히 "모를 것"으로 오판됨)
      member.changeSocial(false);
    }

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
  public String getPhoneCheckStatus(String phone) {

    List<MemberDetail> details = memberDetailRepository.findAllByPhone(phone);

    if (details.isEmpty()) {
      return "AVAILABLE";
    }

    boolean blocked = details.stream()
            .map(MemberDetail::getMember)
            .anyMatch(m -> "BLACKLIST".equals(m.getStatus()) || "DORMANT".equals(m.getStatus()));

    return blocked ? "BLOCKED" : "UNAVAILABLE";
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

      // 그 사이에 같은 번호로 다른 회원이 먼저 가입을 완료했을 수 있으니 마지막에 한 번 더 확인
      String phoneStatus = getPhoneCheckStatus(joinDTO.getPhone());

      if ("BLOCKED".equals(phoneStatus)) {
        return "PHONE_BLOCKED";
      }

      if ("UNAVAILABLE".equals(phoneStatus)) {
        return "PHONE_DUPLICATE";
      }

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

  @Override
  public void withdrawMember(String email) {

    Member member = memberRepository.findById(email).orElseThrow();

    member.withdraw();
    memberRepository.save(member);

    memberDetailRepository.getByMemberEmail(email).ifPresent(detail -> {
      detail.anonymize();
      memberDetailRepository.save(detail);
    });

    socialAccountRepository.deleteAllByMember_Email(email);

    log.info("member withdrawn: " + email);
  }

  @Override
  public void linkKakaoAccount(String email, String kakaoAccessToken) {

    Map<String, String> kakaoInfo = getKakaoUserInfo(kakaoAccessToken);
    String providerId = kakaoInfo.get("providerId");

    Optional<SocialAccount> existing = socialAccountRepository.findByProviderAndProviderId("kakao", providerId);

    if (existing.isPresent()) {
      if (existing.get().getMember().getEmail().equals(email)) {
        // 이미 본인 계정에 연동되어 있음 - 그냥 통과
        return;
      }
      throw new IllegalStateException("이미 다른 계정에 연동된 카카오 계정입니다.");
    }

    Member member = memberRepository.findById(email).orElseThrow();

    SocialAccount socialAccount = SocialAccount.builder()
            .member(member)
            .provider("kakao")
            .providerId(providerId)
            .build();

    socialAccountRepository.save(socialAccount);

    log.info("kakao account linked: " + email);
  }

  @Override
  public void unlinkSocialAccount(String email, String provider) {

    Member member = memberRepository.findById(email).orElseThrow();

    List<SocialAccount> linked = socialAccountRepository.findAllByMember_Email(email);

    SocialAccount target = linked.stream()
            .filter(sa -> sa.getProvider().equals(provider))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("연동되어 있지 않습니다."));

    // 원래 소셜 가입(비밀번호를 본인이 모를 가능성이 높음)인데 이게 마지막 남은 연동이면,
    // 해제 시 로그인 수단이 아예 사라지므로 차단
    if (linked.size() == 1 && member.isSocial()) {
      throw new IllegalStateException(
              "최소 하나의 로그인 수단이 필요합니다. 먼저 비밀번호를 설정한 후 연동 해제해주세요.");
    }

    socialAccountRepository.delete(target);

    log.info(provider + " account unlinked: " + email);
  }

  @Override
  public List<String> getLinkedProviders(String email) {

    return socialAccountRepository.findAllByMember_Email(email)
            .stream()
            .map(SocialAccount::getProvider)
            .toList();
  }
}