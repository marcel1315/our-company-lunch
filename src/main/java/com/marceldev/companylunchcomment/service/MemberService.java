package com.marceldev.companylunchcomment.service;

import com.marceldev.companylunchcomment.component.EmailSender;
import com.marceldev.companylunchcomment.dto.member.ChangePasswordDto;
import com.marceldev.companylunchcomment.dto.member.SecurityMember;
import com.marceldev.companylunchcomment.dto.member.SendVerificationCodeDto;
import com.marceldev.companylunchcomment.dto.member.SignInDto;
import com.marceldev.companylunchcomment.dto.member.SignInResult;
import com.marceldev.companylunchcomment.dto.member.SignUpDto;
import com.marceldev.companylunchcomment.dto.member.UpdateMemberDto;
import com.marceldev.companylunchcomment.dto.member.VerifyVerificationCodeDto;
import com.marceldev.companylunchcomment.dto.member.WithdrawMemberDto;
import com.marceldev.companylunchcomment.entity.Member;
import com.marceldev.companylunchcomment.entity.Verification;
import com.marceldev.companylunchcomment.exception.member.AlreadyExistMemberException;
import com.marceldev.companylunchcomment.exception.member.IncorrectPasswordException;
import com.marceldev.companylunchcomment.exception.member.MemberNotExistException;
import com.marceldev.companylunchcomment.exception.member.MemberUnauthorizedException;
import com.marceldev.companylunchcomment.exception.member.VerificationCodeNotFoundException;
import com.marceldev.companylunchcomment.repository.member.MemberRepository;
import com.marceldev.companylunchcomment.repository.verification.VerificationRepository;
import com.marceldev.companylunchcomment.type.Role;
import com.marceldev.companylunchcomment.util.GenerateVerificationCodeUtil;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService implements UserDetailsService {

  private static final int VERIFICATION_CODE_VALID_SECOND = 60 * 3;

  private static final int VERIFICATION_CODE_LENGTH = 6;

  private final MemberRepository memberRepository;

  private final PasswordEncoder passwordEncoder;

  private final EmailSender emailSender;

  private final VerificationRepository verificationRepository;

  /**
   * 회원가입
   */
  @Transactional
  public void signUp(SignUpDto dto) {
    checkAlreadyExistsMember(dto.getEmail());

    String encPassword = passwordEncoder.encode(dto.getPassword());
    Member member = Member.builder()
        .email(dto.getEmail())
        .password(encPassword)
        .name(dto.getName())
        .role(Role.VIEWER)
        .build();

    memberRepository.save(member);
  }

  /**
   * 로그인
   */
  public SignInResult signIn(SignInDto dto) {
    Member member = memberRepository.findByEmail(dto.getEmail())
        .orElseThrow(MemberNotExistException::new);

    if (!passwordEncoder.matches(dto.getPassword(), member.getPassword())) {
      throw new IncorrectPasswordException();
    }

    return new SignInResult(member.getEmail(), member.getRole());
  }

  /**
   * 이메일로 인증번호 전송
   */
  @Transactional
  public void sendVerificationCode(SendVerificationCodeDto dto) {
    String email = dto.getEmail();
    String code = GenerateVerificationCodeUtil.generate(VERIFICATION_CODE_LENGTH);

    sendVerificationCodeEmail(email, code);
    saveVerificationCodeToDb(email, code);
  }

  /**
   * 인증번호 검증
   */
  @Transactional
  public void verifyVerificationCode(VerifyVerificationCodeDto dto) {
    Verification verification = verificationRepository.findByEmail(dto.getEmail())
        .orElseThrow(VerificationCodeNotFoundException::new);

    matchVerificationCode(dto.getCode(), verification, dto.getNow());

    Member member = getMember();
    member.promoteToEditor();

    verificationRepository.delete(verification);
  }

  /**
   * 회원정보 수정
   */
  @Transactional
  public void updateMember(long id, UpdateMemberDto dto) {
    Member member = getMember(id);
    member.setName(dto.getName());
  }

  /**
   * 회원 비밀번호 수정
   */
  @Transactional
  public void changePassword(long id, ChangePasswordDto dto) {
    Member member = getMember(id);
    if (!passwordEncoder.matches(dto.getOldPassword(), member.getPassword())) {
      throw new IncorrectPasswordException();
    }
    member.setPassword(passwordEncoder.encode(dto.getNewPassword()));
  }

  /**
   * 회원 탈퇴
   */
  @Transactional
  public void withdrawMember(long id, WithdrawMemberDto dto) {
    Member member = getMember(id);
    if (!passwordEncoder.matches(dto.getPassword(), member.getPassword())) {
      throw new IncorrectPasswordException();
    }
    memberRepository.delete(member);
  }

  /**
   * Spring Security 의 UserDetailsService 의 메서드 구현 Spring Security 의 username 으로 해당 서비스의 email 이
   * 사용됨
   */
  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    return memberRepository.findByEmail(email)
        .map(SecurityMember::new)
        .orElseThrow(
            () -> new UsernameNotFoundException(String.format("Member email not found: %s", email))
        );
  }

  @Scheduled(cron = "${scheduler.clear-verification-code.cron}")
  public void clearUnusedVerificationCodes() {
    int rows = verificationRepository.deleteAllExpiredVerificationCode(LocalDateTime.now());
    log.info("MemberService.clearUnusedVerificationCodes executed: {} rows deleted", rows);
  }

  private void checkAlreadyExistsMember(String email) {
    if (memberRepository.existsByEmail(email)) {
      throw new AlreadyExistMemberException();
    }
  }

  private void sendVerificationCodeEmail(String email, String code) {
    String subject = "[Our Company Lunch] 인증번호입니다.";
    String body = String.format("인증번호는 %s 입니다. 인증번호란에 입력해주세요.", code);
    emailSender.sendMail(email, subject, body);
  }

  private void saveVerificationCodeToDb(String email, String code) {
    // 기존에 있다면 제거
    verificationRepository.findByEmail(email).ifPresent(verificationRepository::delete);

    // 새로운 인증번호 저장
    Verification verification = Verification.builder()
        .code(code)
        .expirationAt(LocalDateTime.now().plusSeconds(VERIFICATION_CODE_VALID_SECOND))
        .email(email)
        .build();

    verificationRepository.save(verification);
  }

  private void matchVerificationCode(String code, Verification verification, LocalDateTime now) {
    if (now.isAfter(verification.getExpirationAt())) {
      throw new VerificationCodeNotFoundException();
    }

    if (!verification.getCode().equals(code)) {
      throw new VerificationCodeNotFoundException();
    }
  }

  /**
   * member 를 찾아 반환함. 토큰에 들어있던 사용자가 접근할 수 있는 member id 인지 체크하고 반환함
   */
  private Member getMember(long id) {
    UserDetails user = (UserDetails) SecurityContextHolder.getContext()
        .getAuthentication()
        .getPrincipal();
    String email = user.getUsername();
    return memberRepository.findByIdAndEmail(id, email)
        .orElseThrow(MemberUnauthorizedException::new);
  }

  /**
   * member 를 찾아 반환함.
   */
  private Member getMember() {
    UserDetails user = (UserDetails) SecurityContextHolder.getContext()
        .getAuthentication()
        .getPrincipal();
    String email = user.getUsername();
    return memberRepository.findByEmail(email)
        .orElseThrow(MemberNotExistException::new);
  }
}
