package kr.co.api.flobankapi.controller;

import kr.co.api.flobankapi.dto.CustAcctDTO;
import kr.co.api.flobankapi.dto.CustInfoDTO;
import kr.co.api.flobankapi.jwt.CustomUserDetails;
import kr.co.api.flobankapi.service.MypageService;
import kr.co.api.flobankapi.service.TermsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping("/mypage")
public class MypageController {
    private final TermsService termsService;
    private final MypageService mypageService;

    @GetMapping({"/main","/"})
    public String mypage() {
        return "mypage/main";
    }

    @GetMapping("/account_open_main")
    public String account_open_main() {
        return "mypage/account_open_main";
    }

    @GetMapping("/ko_account_open_1")
    public String openAccountTerms(Model model) {

//        model.addAttribute("termsType1", termsService.getTermsByType(1));
//        model.addAttribute("termsType2", termsService.getTermsByType(2));
//        model.addAttribute("termsType3", termsService.getTermsByType(3));
//        model.addAttribute("termsType4", termsService.getTermsByType(4));

        return "mypage/ko_account_open_1";  // Thymeleaf 템플릿 경로
    }

    @GetMapping("/ko_account_open_2")
    public String ko_account_open_2(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {

        if(userDetails == null) {
            return "redirect:/login";
        }

        String userId = userDetails.getUsername();
        log.info("userId = " + userId);
        CustInfoDTO custInfoDTO = mypageService.getCustInfo(userId);

        // 보안 등급별 1회, 1일 이체한도 고시
        if(custInfoDTO.getCustSecurityLevel().equals(1)) {
            model.addAttribute("dayTrsfLmt", "5억");
            model.addAttribute("onceTrsfLmt", "1억");
        }else if(custInfoDTO.getCustSecurityLevel().equals(2)) {
            model.addAttribute("dayTrsfLmt", "5천만원");
            model.addAttribute("onceTrsfLmt", "1천만원");
        } else if(custInfoDTO.getCustSecurityLevel().equals(3)) {
            model.addAttribute("dayTrsfLmt", "1천만원");
            model.addAttribute("onceTrsfLmt", "1천만원");
        }

        // '남자', '여자'
        if("M".equals(custInfoDTO.getCustGen())) {
            model.addAttribute("gender", "남자");
        } else {
            model.addAttribute("gender", "여자");
        }

        // 생년월일 마스킹
        String maskedBirth = "";

        if (custInfoDTO.getCustBirthDt() != null) {
            String birthString = custInfoDTO.getCustBirthDt().toString(); //
            maskedBirth = birthString.substring(0, 4) + "-**-**";
        }
        model.addAttribute("maskedBirthDt", maskedBirth);

        model.addAttribute("custInfoDTO", custInfoDTO);
        model.addAttribute("custAcctDTO", new CustAcctDTO()); // View의 폼이 사용할 빈 객체 전달

        return "mypage/ko_account_open_2";
    }

    @PostMapping("/ko_account_open_2")
    public String createAcct(@ModelAttribute CustAcctDTO custAcctDTO, @AuthenticationPrincipal CustomUserDetails userDetails){

        log.info("custAcctDTO = " + custAcctDTO);
        mypageService.saveAcct(custAcctDTO);

        return "redirect:/mypage/ko_account_open_3";
    }

    @GetMapping("/ko_account_open_3")
    public String ko_account_open_3() {
        return "mypage/ko_account_open_3";
    }

    @GetMapping("/koAcctCheck")
    public String koAcctCheck(@AuthenticationPrincipal CustomUserDetails userDetails, RedirectAttributes redirectAttributes) {

        String userId = userDetails.getUsername();
        log.info("userId = " + userId);
        CustInfoDTO custInfoDTO = mypageService.getCustInfo(userId);
        if(mypageService.checkKoAcct(custInfoDTO.getCustCode())){
            return "redirect:/mypage/ko_account_open_1";
        }else {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "최근 1개월(30일) 이내에 원화 입출금 통장을 개설한 이력이 있어 신규 개설이 제한됩니다. (금융사기 예방 조치)");
            return "redirect:/mypage/account_open_main";
        }

    }

    @GetMapping("/enAcctCheck")
    public String enAcctCheck(@AuthenticationPrincipal CustomUserDetails userDetails, RedirectAttributes redirectAttributes) {
        String userId = userDetails.getUsername();
        log.info("userId = " + userId);
        CustInfoDTO custInfoDTO = mypageService.getCustInfo(userId);
        if(mypageService.checkCntKoAcct(custInfoDTO.getCustCode()) >= 1){
            return "redirect:/mypage/en_account_open_1";
        }else{
            redirectAttributes.addFlashAttribute("errorMessage",
                    "원화 입출금 통장이 1개 이상 있어야 개설 가능합니다. 원화 입출금 통장을 먼저 만들어주세요.");
            return "redirect:/mypage/account_open_main";
        }


    }

    @GetMapping("/en_account_open_1")
    public String en_account_open_1() {

        return "mypage/en_account_open_1";
    }

    @GetMapping("/en_account_open_2")
    public String en_account_open_2() {

        return "mypage/en_account_open_2";
    }

    @GetMapping("/en_account_open_3")
    public String en_account_open_3() {

        return "mypage/en_account_open_3";
    }

    @GetMapping("/chatbot")
    public String chatbot() {
        return "mypage/chatbot";
    }
}
