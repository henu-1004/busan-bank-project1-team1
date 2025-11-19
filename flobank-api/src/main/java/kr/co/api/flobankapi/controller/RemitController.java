package kr.co.api.flobankapi.controller;

import kr.co.api.flobankapi.jwt.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/remit")
public class RemitController {
    @GetMapping("/en_transfer_1")
    public String en_transfer_1(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        return  "remit/en_transfer_1";
    }

    @GetMapping("/en_transfer_2")
    public String en_transfer_2(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        return  "remit/en_transfer_2";
    }

    @GetMapping("/en_transfer_3")
    public String en_transfer_3(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        return  "remit/en_transfer_3";
    }

    @GetMapping("/en_transfer_4")
    public String en_transfer_4(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        return  "remit/en_transfer_4";
    }
}
