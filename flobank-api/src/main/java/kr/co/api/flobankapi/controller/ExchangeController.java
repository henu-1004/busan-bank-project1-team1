/*
* 날짜 : 2025/11/20
* 이름 : 김대현
* 내용 : 디비 불러오기 수정
* */


package kr.co.api.flobankapi.controller;

import kr.co.api.flobankapi.dto.CustAcctDTO;
import kr.co.api.flobankapi.service.ExchangeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import kr.co.api.flobankapi.service.TermsDbService;
import kr.co.api.flobankapi.service.RateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/exchange")
@RequiredArgsConstructor
public class ExchangeController {

    private final ExchangeService exchangeService;
    private final TermsDbService termsDbService;
    private final RateService rateService;


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

    @GetMapping("/api/rate")
    @ResponseBody
    public ResponseEntity<?> getCurrentRate(@RequestParam String currency) {
        Map<String, Object> response = new HashMap<>();

        try {
            // ✅ RateService를 통해 Redis에 있는 데이터를 뒤져서 환율을 가져옴
            double rate = rateService.getCurrencyRate(currency);

            if (rate == 0.0) {
                response.put("status", "error");
                response.put("message", "현재 환율 정보를 불러올 수 없습니다. (휴일이거나 데이터 없음)");
                // 실제 서비스에선 '최근 영업일' 데이터를 가져오는 로직을 Service에 추가해야 함
            } else {
                response.put("status", "success");
                response.put("rate", rate);
                response.put("currency", currency);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("환율 조회 API 에러", e);
            response.put("status", "error");
            response.put("message", "서버 오류가 발생했습니다.");
            return ResponseEntity.badRequest().body(response);
        }
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
    public String step2(@AuthenticationPrincipal UserDetails user, Model model){
        String userCode = user.getUsername();
        List<CustAcctDTO> custAcctDTOList = exchangeService.getAllKoAcct(userCode);
        model.addAttribute("custAcctDTOList",custAcctDTOList);


        return "exchange/step2";
    }

    @GetMapping("/step3")
    public String step3(){
        return "exchange/step3";
    }
}
