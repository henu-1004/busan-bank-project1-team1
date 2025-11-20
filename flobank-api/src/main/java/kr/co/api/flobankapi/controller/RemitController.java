package kr.co.api.flobankapi.controller;

import kr.co.api.flobankapi.dto.CustAcctDTO;
import kr.co.api.flobankapi.dto.CustFrgnAcctDTO;
import kr.co.api.flobankapi.dto.FrgnAcctBalanceDTO;
import kr.co.api.flobankapi.dto.FrgnRemtTranDTO;
import kr.co.api.flobankapi.jwt.CustomUserDetails;
import kr.co.api.flobankapi.mapper.RemitMapper;
import kr.co.api.flobankapi.service.MypageService;
import kr.co.api.flobankapi.service.RemitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/remit")
@RequiredArgsConstructor
public class RemitController {
    private final MypageService mypageService;
    private final RemitService remitService;

    @GetMapping("/en_transfer_1")
    public String en_transfer_1(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {


        return  "remit/en_transfer_1";
    }

    @GetMapping("/en_transfer_2")
    public String en_transfer_2(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        String userCode = userDetails.getUsername();
        List<CustAcctDTO> custAcctDTOList = mypageService.findAllAcct(userCode);
        CustFrgnAcctDTO custFrgnAcctDTO = mypageService.findFrgnAcct(userCode);

        List<FrgnAcctBalanceDTO> frgnAcctBalanceDTOList = mypageService.getAllFrgnAcctBal(custFrgnAcctDTO.getFrgnAcctNo());

        // 1. 자식 통장 잔액 전달 (통화 : 잔액)
        Map<String, Integer> currencyAcctBal = new HashMap<>();
        // 2. 자식 통장 계좌번호 전달 (통화 : 자식계좌번호)
        Map<String, String> currencyAcctNo = new HashMap<>();

        for(FrgnAcctBalanceDTO dto : frgnAcctBalanceDTOList){
            // DTO의 balCurrency(통화)를 Key로 사용
            currencyAcctBal.put(dto.getBalCurrency(), dto.getBalBalance());

            // DTO의 balNo(자식 계좌번호)를 Value로 저장
            currencyAcctNo.put(dto.getBalCurrency(), dto.getBalNo());
        }

        model.addAttribute("currencyAcctBal", currencyAcctBal);
        model.addAttribute("currencyAcctNo", currencyAcctNo);

        // 외화 / 원화 계좌 리스트 보내기
        model.addAttribute("custFrgnAcctDTO", custFrgnAcctDTO);
        model.addAttribute("custAcctDTOList", custAcctDTOList);

        // 외화 이체 내역 테이블 전달(정보 이동용)
        FrgnRemtTranDTO frgnRemtTranDTO = new FrgnRemtTranDTO();
        model.addAttribute("frgnRemtTranDTO", frgnRemtTranDTO);
        return  "remit/en_transfer_2";
    }

    @GetMapping("/en_transfer_3")
    public String en_transfer_3(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        return  "remit/en_transfer_3";
    }

    @PostMapping("en_transfer_3")
    public String enTransfer3Post(@ModelAttribute FrgnRemtTranDTO frgnRemtTranDTO, Model model){
        if(frgnRemtTranDTO.getRemtCustName() == null){
            frgnRemtTranDTO.setRemtCustName(frgnRemtTranDTO.getRemtRecAccNo());
        }
        frgnRemtTranDTO.setRemtFee(4900); // 고정 수수료
        frgnRemtTranDTO.setRemtEsignYn("Y");

        remitService.saveFrgnRemtTran(frgnRemtTranDTO);

        // 고객에게 출력될 계좌번호는 모체이기에 다시 불러옴
        CustFrgnAcctDTO custFrgnAcctDTO = remitService.getParAcctNo(frgnRemtTranDTO.getRemtAcctNo());
        model.addAttribute("custFrgnAcctDTO", custFrgnAcctDTO);
        switch (frgnRemtTranDTO.getRemtCurrency()) {
            case "USD":
                model.addAttribute("currency", "(미국 달러)");
                break;
            case "JPY":
                model.addAttribute("currency", "(일본 엔화)");
                break;
            case "EUR":
                model.addAttribute("currency", "(유럽 유로)");
                break;
            case "CNH":
                model.addAttribute("currency", "(중국 위안화)");
                break;
            case "GBP":
                model.addAttribute("currency", "(영국 파운드)");
                break;
            case "AUD":
                model.addAttribute("currency", "(호주 달러)");
                break;
            default:
                model.addAttribute("currency", "(기타 통화)");
                break;
        }

        // 통화별 기호(Symbol) 매핑
        switch (frgnRemtTranDTO.getRemtCurrency()) {
            case "USD":
                model.addAttribute("currencySymbol", "$");   // 미국 달러
                break;
            case "JPY":
                model.addAttribute("currencySymbol", "¥");   // 일본 엔화
                break;
            case "EUR":
                model.addAttribute("currencySymbol", "€");   // 유로
                break;
            case "CNH":
                model.addAttribute("currencySymbol", "¥");   // 중국 위안화
                break;
            case "GBP":
                model.addAttribute("currencySymbol", "£");   // 영국 파운드
                break;
            case "AUD":
                model.addAttribute("currencySymbol", "$");   // 호주 달러 (구분이 필요하면 "A$" 사용 가능)
                break;
            default:
                model.addAttribute("currencySymbol", frgnRemtTranDTO.getRemtCurrency()); // 없을 경우 통화코드 그대로 출력
                break;
        }

        if("Y".equals(frgnRemtTranDTO.getRemtEsignYn())){
            model.addAttribute("state", "정상");
            model.addAttribute("frgnRemtTranDTO", frgnRemtTranDTO);
        }else {
            model.addAttribute("state", "실패");
            model.addAttribute("frgnRemtTranDTO", frgnRemtTranDTO);
        }

        log.info("frgnRemtTranDTO = {}", frgnRemtTranDTO);

        return "remit/en_transfer_3";
    }

    @GetMapping("/en_transfer_4")
    public String en_transfer_4(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        return  "remit/en_transfer_4";
    }
}
