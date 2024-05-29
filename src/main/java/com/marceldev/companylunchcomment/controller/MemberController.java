package com.marceldev.companylunchcomment.controller;

import com.marceldev.companylunchcomment.dto.member.SignInDto;
import com.marceldev.companylunchcomment.dto.member.SignUpDto;
import com.marceldev.companylunchcomment.response.CustomResponse;
import com.marceldev.companylunchcomment.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.HashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MemberController {

  private final MemberService memberService;

  @Operation(
      summary = "회원가입",
      description = "사용자는 회원가입을 할 수 있다. 모든 사용자는 회원가입 시 USER 권한(일반 권한)을 지닌다.<br>"
          + "회원가입시 이메일, 이름과 비밀번호를 입력받는다. 이메일이 아이디가 되며 unique 해야한다.<br>"
          + "구글, 네이버, 카카오, 다음, 한메일, 야후 등 이메일 공급자로부터 받은 이메일은 가입할 수 없다. 회사 도메인을 사용해야 한다.<br>"
          + "회원가입 중 이메일을 통한 번호인증을 한다."
  )
  // TODO: 이메일 공급자 예외. 번호인증
  @PostMapping("/signup")
  public ResponseEntity<?> signUp(@Validated @RequestBody SignUpDto signUpDto) {
    memberService.signUp(signUpDto);
    return CustomResponse.success();
  }

  @Operation(
      summary = "로그인",
      description = "사용자는 로그인을 할 수 있다. 로그인시 회원가입에 사용한 아이디(이메일)와 패스워드가 일치해야 한다.\n"
  )
  @PostMapping("/signin")
  public ResponseEntity<?> signIn(@Validated @RequestBody SignInDto signInDto) {
    String token = memberService.signIn(signInDto);
    HashMap<String, String> resultMap = new HashMap<>();
    resultMap.put("token", token);
    return CustomResponse.success(resultMap);
  }
}
