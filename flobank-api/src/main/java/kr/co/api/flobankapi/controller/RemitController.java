package kr.co.api.flobankapi.controller;

import kr.co.api.flobankapi.dto.CustAcctDTO;
import kr.co.api.flobankapi.dto.CustFrgnAcctDTO;
import kr.co.api.flobankapi.jwt.CustomUserDetails;
import kr.co.api.flobankapi.service.MypageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/remit")
@RequiredArgsConstructor
public class RemitController {
    private final MypageService mypageService;

    @GetMapping("/en_transfer_1")
    public String en_transfer_1(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {


        return  "remit/en_transfer_1";
    }

    @GetMapping("/en_transfer_2")
    public String en_transfer_2(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        String userCode = userDetails.getUsername();
        List<CustAcctDTO> custAcctDTOList = mypageService.findAllAcct(userCode);
        CustFrgnAcctDTO custFrgnAcctDTO = mypageService.findFrgnAcct(userCode);

        // 외화 / 원화 계좌 리스트 보내기
        model.addAttribute("custFrgnAcctDTO", custFrgnAcctDTO);
        model.addAttribute("custAcctDTOList", custAcctDTOList);
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
