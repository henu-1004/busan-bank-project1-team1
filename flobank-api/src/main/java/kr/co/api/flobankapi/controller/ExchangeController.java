/*
* 날짜 : 2025/11/20
* 이름 : 김대현
* 내용 : 디비 불러오기 수정
* */


package kr.co.api.flobankapi.controller;

import kr.co.api.flobankapi.service.ExchangeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import kr.co.api.flobankapi.service.TermsDbService;

import java.math.BigDecimal;

@Controller
@RequestMapping("/exchange")
@RequiredArgsConstructor
public class ExchangeController {

    private final ExchangeService exchangeService;
    private final TermsDbService termsDbService;


    // 환전하기
    @ResponseBody
    @GetMapping("/calculate")
    public BigDecimal calculateExchange(@RequestParam String date,
                                        @RequestParam String currency,
                                        @RequestParam BigDecimal amount) {
        // 예: 20231025 날짜의 USD 환율로 1,000,000원을 환전
        return exchangeService.calculateExchange(date, currency, amount);
    }

    @GetMapping("/benefit")
    public String benefit(Model model){
        model.addAttribute("activeItem","benefit");
        return "exchange/benefit";
    }

    @GetMapping("/info1")
    public String info1(Model model){
        model.addAttribute("activeItem","info1");
        return "exchange/info1";
    }

    @GetMapping("/info2")
    public String info2(Model model){
        model.addAttribute("activeItem","info2");
        return "exchange/info2";
    }

    @GetMapping("/info3")
    public String info3(Model model){
        model.addAttribute("activeItem","info3");
        return "exchange/info3";
    }

    //환전하기 약관 불러오기
    @GetMapping("/step1")
    public String step1(Model model){
        int termLocation = 2; // 2번: 환전하기

        model.addAttribute("termsList",
                termsDbService.getTermsByLocation(termLocation)
        );

        return "exchange/step1";
    }

    @GetMapping("/step2")
    public String step2(){
        return "exchange/step2";
    }

    @GetMapping("/step3")
    public String step3(){
        return "exchange/step3";
    }
}
