/*
 * 날짜 : 2025/11/20
 * 이름 : 김대현
 * 내용 : 디비 불러오기 수정
 * */

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
import kr.co.api.flobankapi.service.TermsDbService;

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
    private final TermsDbService termsDbService;

    private final RemitService remitService;

    @GetMapping("/en_transfer_1")
    public String en_transfer_1(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {

        int termLocation = 3; // 3번: 외화송금

        model.addAttribute("termsList",
                termsDbService.getTermsByLocation(termLocation)
        );


        return  "remit/en_transfer_1";
    }

    @GetMapping("/en_transfer_2")
    public String en_transfer_2(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        String userCode = userDetails.getUsername();
        List<CustAcctDTO> custAcctDTOList = mypageService.findAllAcct(userCode);
        CustFrgnAcctDTO custFrgnAcctDTO = mypageService.findFrgnAcct(userCode);

        List<FrgnAcctBalanceDTO> frgnAcctBalanceDTOList = mypageService.getAllFrgnAcctBal(custFrgnAcctDTO.getFrgnAcctNo());

        // 1. 자식 통장 잔액 전달 (통화 : 잔액)
        Map<String, Double> currencyAcctBal = new HashMap<>();
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

        frgnRemtTranDTO.setRemtEsignYn("Y");

        // 고객에게 출력될 계좌번호는 모체이기에 다시 불러옴
        CustFrgnAcctDTO custFrgnAcctDTO = remitService.getParAcctNo(frgnRemtTranDTO.getRemtAcctNo());
        model.addAttribute("custFrgnAcctDTO", custFrgnAcctDTO);

        if("Y".equals(frgnRemtTranDTO.getRemtEsignYn())){
            model.addAttribute("state", "정상");
            model.addAttribute("frgnRemtTranDTO", frgnRemtTranDTO);
        }else {
            model.addAttribute("state", "실패");
            model.addAttribute("frgnRemtTranDTO", frgnRemtTranDTO);
        }

        // 원화 계좌일 때의 수수료 / 각 통화별 수수료 설정
        if(frgnRemtTranDTO.getRemtAcctNo().contains("-10-")){
            model.addAttribute("currency", "(한국 원화)");
            model.addAttribute("currencySymbol", "₩");
            frgnRemtTranDTO.setRemtFee(4900); // 고정 수수료
        } else{
            switch (frgnRemtTranDTO.getRemtCurrency()) {
                case "USD":
                    model.addAttribute("currency", "(미국 달러)");
                    model.addAttribute("currencySymbol", "$");   // 미국 달러
                    frgnRemtTranDTO.setRemtFee(3.34);
                    break;
                case "JPY":
                    model.addAttribute("currency", "(일본 엔화)");
                    model.addAttribute("currencySymbol", "¥");   // 일본 엔화
                    frgnRemtTranDTO.setRemtFee(526.33);
                    break;
                case "EUR":
                    model.addAttribute("currency", "(유럽 유로)");
                    model.addAttribute("currencySymbol", "€");   // 유로
                    frgnRemtTranDTO.setRemtFee(2.89);
                    break;
                case "CNH":
                    model.addAttribute("currency", "(중국 위안화)");
                    model.addAttribute("currencySymbol", "¥");   // 중국 위안화
                    frgnRemtTranDTO.setRemtFee(23.72);
                    break;
                case "GBP":
                    model.addAttribute("currency", "(영국 파운드)");
                    model.addAttribute("currencySymbol", "£");   // 영국 파운드
                    frgnRemtTranDTO.setRemtFee(2.55);
                    break;
                case "AUD":
                    model.addAttribute("currency", "(호주 달러)");
                    model.addAttribute("currencySymbol", "$");   // 호주 달러 (구분이 필요하면 "A$" 사용 가능)
                    frgnRemtTranDTO.setRemtFee(5.14);
                    break;
                default:
                    break;
            }
        }

        log.info("frgnRemtTranDTO = {}", frgnRemtTranDTO);

        return "remit/en_transfer_3";
    }

    @PostMapping("/en_transfer_4")
    public String en_transfer_4(@ModelAttribute FrgnRemtTranDTO frgnRemtTranDTO, Model model) {

        // 1. 이체 승인 여부 설정 (Y)
        frgnRemtTranDTO.setRemtEsignYn("Y");

        // 2. DB 저장 (Service 호출)
        boolean check = remitService.saveFrgnTran(frgnRemtTranDTO);

        // 3. 완료 페이지 보여주기 위한 데이터 세팅
        // 3-1. 출금 계좌 정보(모체 계좌) 다시 조회
        CustFrgnAcctDTO custFrgnAcctDTO = remitService.getParAcctNo(frgnRemtTranDTO.getRemtAcctNo());
        model.addAttribute("custFrgnAcctDTO", custFrgnAcctDTO);

        // 3-2. 통화 기호 및 수수료 정보 다시 세팅 (화면에 보여주기 위함)
        String currencySymbol = "";
        String currencyName = "";

        if(frgnRemtTranDTO.getRemtAcctNo().contains("-10-")){
            currencyName = "(한국 원화)";
            currencySymbol = "₩";
        } else {
            switch (frgnRemtTranDTO.getRemtCurrency()) {
                case "USD": currencyName = "(미국 달러)"; currencySymbol = "$"; break;
                case "JPY": currencyName = "(일본 엔화)"; currencySymbol = "¥"; break;
                case "EUR": currencyName = "(유럽 유로)"; currencySymbol = "€"; break;
                case "CNH": currencyName = "(중국 위안화)"; currencySymbol = "¥"; break;
                case "GBP": currencyName = "(영국 파운드)"; currencySymbol = "£"; break;
                case "AUD": currencyName = "(호주 달러)"; currencySymbol = "$"; break;
            }
        }

        model.addAttribute("currency", currencyName);
        model.addAttribute("currencySymbol", currencySymbol);

        // 결과 상태
        if(check){ // DB 결과 정상적으로 작동하지 않았는데 여기로 왔다면 실패
            model.addAttribute("state", "정상");
        }else{
            model.addAttribute("state", "실패");
        }
        model.addAttribute("frgnRemtTranDTO", frgnRemtTranDTO);
        log.info("frgnRemtTranDTO = {}", frgnRemtTranDTO);
        return "remit/en_transfer_4";
    }
}
