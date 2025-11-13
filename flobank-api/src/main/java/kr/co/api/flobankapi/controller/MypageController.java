package kr.co.api.flobankapi.controller;

import kr.co.api.flobankapi.dto.CustAcctDTO;
import kr.co.api.flobankapi.service.TermsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/mypage")
public class MypageController {

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

        model.addAttribute("termsType1", TermsService.getTermsByType(1));
        model.addAttribute("termsType2", TermsService.getTermsByType(2));
        model.addAttribute("termsType3", TermsService.getTermsByType(3));
        model.addAttribute("termsType4", TermsService.getTermsByType(4));

        return "mypage/ko_account_open_1";  // Thymeleaf 템플릿 경로
    }

    @GetMapping("/ko_account_open_2")
    public String ko_account_open_2(@ModelAttribute CustAcctDTO custAcctDTO) {
        return "mypage/ko_account_open_2";
    }

    @GetMapping("/ko_account_open_3")
    public String ko_account_open_3() {
        return "mypage/ko_account_open_3";
    }

    @GetMapping("/chatbot")
    public String chatbot() {
        return "mypage/chatbot";
    }
}
