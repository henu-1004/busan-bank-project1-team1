package kr.co.api.flobankapi.controller;

import kr.co.api.flobankapi.dto.CustAcctDTO;
import org.springframework.stereotype.Controller;
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
    public String ko_account_open_1() {
        return "mypage/ko_account_open_1";
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
