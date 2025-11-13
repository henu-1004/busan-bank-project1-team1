package kr.co.api.flobankapi.controller;

// 1. 필요한 클래스 임포트
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.api.flobankapi.dto.ApResponseDTO;
import kr.co.api.flobankapi.dto.CustInfoDTO;
import kr.co.api.flobankapi.dto.MemberDTO;
import kr.co.api.flobankapi.jwt.JwtTokenProvider;
import kr.co.api.flobankapi.service.CustInfoService;
import kr.co.api.flobankapi.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HttpServletBean;
import org.springframework.web.servlet.mvc.support.RedirectAttributes; // 2. RedirectAttributes 임포트

@Slf4j
@Controller
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final CustInfoService custInfoService;
    private final JwtTokenProvider jwtTokenProvider;

    // 3. registerPage 메소드가 에러 메시지를 받을 수 있도록 수정
    @GetMapping("/register")
    public String registerPage(Model model, @ModelAttribute("errorMessage") String errorMessage) {
        if (!model.containsAttribute("memberDTO")) {
            model.addAttribute("memberDTO", new MemberDTO());
        }
        // FlashAttribute로 전달된 에러 메시지가 있으면 모델에 추가
        if (errorMessage != null && !errorMessage.isEmpty()) {
            model.addAttribute("errorMessage", errorMessage);
        }
        return "member/register"; // templates/member/register.html
    }

    /**
     * 회원가입 처리 (POST)
     * th:object="${memberDTO}"로 보낸 폼 데이터를 @ModelAttribute MemberDTO memberDTO로 받습니다.
     */
    // 4. registerProcess 메소드가 응답을 처리하도록 수정
    @PostMapping("/register")
    public String registerProcess(@ModelAttribute MemberDTO memberDTO, RedirectAttributes redirectAttributes) {

        // 1. DTO가 폼 데이터를 제대로 받았는지 로그로 확인
        log.info("[회원가입 시도] : {}", memberDTO.toString());

        // 2. MemberService를 호출하여 응답을 받음
        ApResponseDTO response = memberService.register(memberDTO);

        // 3. 응답 상태에 따라 분기
        if ("SUCCESS".equals(response.getStatus())) {
            // 3-1. 성공 시 완료 페이지로
            return "redirect:/member/complete";
        } else {
            // 3-2. 실패 시 회원가입 폼으로 다시 보내고 에러 메시지 전달
            log.warn("[회원가입 실패] Message: {}", response.getMessage());
            // RedirectAttributes에 에러 메시지를 담아 전달 (1회성)
            redirectAttributes.addFlashAttribute("errorMessage", response.getMessage());
            redirectAttributes.addFlashAttribute("memberDTO", memberDTO); // 사용자가 입력했던 값 유지
            return "redirect:/member/register"; // 다시 회원가입 폼으로
        }
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
                        HttpServletResponse response) {

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
            return "redirect:/"; // 메인으로 이동
        }else {
            return "redirect:/member/login?error"; // 로그인 실패
        }
    }

    @PostMapping("/logout")
    public String logout(HttpServletResponse response) {
        // 로그아웃 시 쿠키 삭제 (만료시간 0으로 재설정하여 덮어쓰기)
        Cookie cookie = new Cookie("accessToken", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);

        return "redirect:/";
    }
}