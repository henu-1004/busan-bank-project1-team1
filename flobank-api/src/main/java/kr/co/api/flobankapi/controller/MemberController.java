package kr.co.api.flobankapi.controller;

// 1. 필요한 클래스 임포트
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kr.co.api.flobankapi.dto.ApResponseDTO;
import kr.co.api.flobankapi.dto.CustInfoDTO;
import kr.co.api.flobankapi.dto.MemberDTO;
import kr.co.api.flobankapi.jwt.JwtTokenProvider;
import kr.co.api.flobankapi.service.CustInfoService;
import kr.co.api.flobankapi.service.EmailService;
import kr.co.api.flobankapi.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HttpServletBean;
import org.springframework.web.servlet.mvc.support.RedirectAttributes; // 2. RedirectAttributes 임포트

import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {

    private final CustInfoService custInfoService;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;

    @GetMapping("/terms")
    public String termsPage() {
        return "member/terms";
    }

    // 3. registerPage 메소드가 에러 메시지를 받을 수 있도록 수정
    @GetMapping("/register")
    public String registerPage(Model model) {

        model.addAttribute("custInfoDTO", new CustInfoDTO());

        return "member/register"; // templates/member/register.html
    }

    /*
      회원가입 처리 (POST)
      th:object="${memberDTO}"로 보낸 폼 데이터를 @ModelAttribute MemberDTO memberDTO로 받습니다.
     */
    // 4. registerProcess 메소드가 응답을 처리하도록 수정
    @PostMapping("/register")
    public String registerProcess(@ModelAttribute CustInfoDTO custInfoDTO) {
        log.info("custInfoDTO={}", custInfoDTO);
        if(custInfoDTO != null){
            custInfoService.register(custInfoDTO);
            return "redirect:/member/complete";
        }else {
            log.error("custInfoDTO가 널 {} ", custInfoDTO);
            return "member/register";
        }

    }

    /*
        회원가입 - 아이디 유효성 검사
     */
    @PostMapping("/checkId")
    @ResponseBody
    public Boolean checkId(@RequestParam("custId") String custId) {
        return custInfoService.checkId(custId);
    }

    // 5. /member/complete GET 매핑 추가
    @GetMapping("/complete")
    public String completePage() {
        return "member/complete";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "member/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam("userid") String userid,
                        @RequestParam("password") String password,
                        HttpServletResponse response,
                        HttpServletRequest request) {

        // 1. 회원 정보 확인 (ID/PW 검증) - CustInfoService 내부에서 검증 로직 수행 가정
        CustInfoDTO custInfoDTO = custInfoService.login(userid, password);

        if (custInfoDTO != null) {
            // 2. 토큰 생성
            String token = jwtTokenProvider.createToken(custInfoDTO.getCustId(), "USER", custInfoDTO.getCustName());

            // 3. 쿠키 생성 및 설정
            Cookie cookie = new Cookie("accessToken", token);
            cookie.setHttpOnly(true); // 자바스크립트 접근 차단 (보안 필수)
            cookie.setSecure(false); // https 적용 시 true로 변경
            cookie.setPath("/"); // 모든 경로에서 접근 가능
            cookie.setMaxAge(3600); // 1시간(토큰 만료시간과 맞춤)

            // 4. 응답에 쿠키 추가
            response.addCookie(cookie);

            // 프론트에서 체크할 로그인 플래그 쿠키
            Cookie loginFlag = new Cookie("loginYn", "Y");
            loginFlag.setHttpOnly(false); // JS에서 읽을 수 있게
            loginFlag.setPath("/");
            loginFlag.setMaxAge(3600);
            response.addCookie(loginFlag);

            return "redirect:/"; // 메인으로 이동
        }else {
            return "redirect:/member/login?error"; // 로그인 실패
        }
    }

    @PostMapping("/logout")
    public String logout(HttpServletResponse response, HttpServletRequest request) {
        // 세션 만료
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        // 로그아웃 시 쿠키 삭제 (만료시간 0으로 재설정하여 덮어쓰기)
        Cookie cookie = new Cookie("accessToken", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
        // loginYn 쿠키 삭제
        Cookie loginFlag = new Cookie("loginYn", null);
        loginFlag.setPath("/");
        loginFlag.setMaxAge(0);
        response.addCookie(loginFlag);

        return "redirect:/";
    }
}