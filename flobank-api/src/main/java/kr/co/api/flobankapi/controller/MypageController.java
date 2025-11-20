package kr.co.api.flobankapi.controller;

import kr.co.api.flobankapi.dto.*;

import kr.co.api.flobankapi.dto.SearchResDTO;
import kr.co.api.flobankapi.dto.TermsHistDTO;
import kr.co.api.flobankapi.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import kr.co.api.flobankapi.jwt.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Controller
@RequestMapping("/mypage")
@Slf4j
@RequiredArgsConstructor
public class MypageController {
    private final TermsDbService termsService;
    private final MypageService mypageService;

    private final QTypeClassifierService typeClassifier;
    private final EmbeddingService embeddingService;
    private final PineconeService pineconeService;
    private final ChatGPTService chatGPTService;


    @GetMapping({"/main","/"})
    public String mypage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {

        String userCode = userDetails.getUsername();
        List<CustAcctDTO> custAcctDTOList = mypageService.findAllAcct(userCode);
        CustFrgnAcctDTO  custFrgnAcctDTO = mypageService.findFrgnAcct(userCode);

        model.addAttribute("custAcctDTOList",custAcctDTOList);
        model.addAttribute("custFrgnAcctDTO",custFrgnAcctDTO);

        return "mypage/main";
    }

    @PostMapping("/updateAcctName")
    public ResponseEntity<?> updateAcctName(@RequestBody AcctNameUpdateRequestDTO requestDTO) {

        Map<String, Object> response = new HashMap<>();
        log.info("updateAcctName requestDTO:{}", requestDTO);
        try {
            // --- 비즈니스 로직 ---
            // 1. 유효성 검사 (예: requestDTO.getAcctName()의 길이 등)
            // (JavaScript에서 이미 검사했지만, 서버에서도 이중 검사하는 것이 안전합니다)
            String newName = requestDTO.getAcctName();
            if (newName == null || newName.trim().isEmpty() || newName.length() > 20) {
                response.put("status", "error");
                response.put("message", "별명은 1자 이상 20자 이하로 입력하세요.");
                // 400 Bad Request 상태와 에러 메시지를 반환
                return ResponseEntity.badRequest().body(response);
            }

            if ("KRW".equals(requestDTO.getAcctType())){
                mypageService.modifyAcctName(requestDTO.getAcctName(), requestDTO.getAcctNo());
            }else{
                mypageService.modifyFrgnAcctName(requestDTO.getAcctName(), requestDTO.getAcctNo());
            }

            // 3. 성공 응답 반환
            response.put("status", "success");
            // 200 OK 상태와 성공 메시지를 반환
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("계좌 별명 변경 중 심각한 오류 발생", e);
            // 4. 예기치 못한 서버 오류 처리
            response.put("status", "error");
            response.put("message", "서버 오류로 인해 별명 변경에 실패했습니다.");
            // 500 Internal Server Error 상태와 에러 메시지를 반환
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/account_open_main")
    public String account_open_main() {
        return "mypage/account_open_main";
    }

    @GetMapping("/ko_account_open_1")
    public String openAccountTerms(Model model) {
        int termLocation = 5; // 5번: 원화통장개설 페이지

        List<TermsHistDTO> termsList = termsService.getTermsByLocation(termLocation);

        System.out.println("### termsList size = " + termsList.size());

        model.addAttribute("termsList", termsList);

        return "mypage/ko_account_open_1";
    }

    @GetMapping("/ko_account_open_2")
    public String ko_account_open_2(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {

        if(userDetails == null) {
            return "redirect:/login";
        }

        String userCode = userDetails.getUsername();
        log.info("userCode = " + userCode);
        CustInfoDTO custInfoDTO = mypageService.getCustInfo(userCode);

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

        String userCode = userDetails.getUsername();
        log.info("userId = " + userCode);
        CustInfoDTO custInfoDTO = mypageService.getCustInfo(userCode);
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
        String UserCode = userDetails.getUsername();
        log.info("UserCode = " + UserCode);
        CustInfoDTO custInfoDTO = mypageService.getCustInfo(UserCode);
        if(mypageService.checkCntKoAcct(custInfoDTO.getCustCode()) >= 1){
            int checkCnt = mypageService.checkEnAcct(UserCode);
            if(checkCnt >= 1){
                redirectAttributes.addFlashAttribute("errorMessage",
                        "이미 외화 입출금통장이 있습니다. 외화 입출금통장은 2개 이상 만들 수 없습니다.");
                return "redirect:/mypage/account_open_main";
            }else {
                return "redirect:/mypage/en_account_open_1";
            }
        }else{
            redirectAttributes.addFlashAttribute("errorMessage",
                    "원화 입출금 통장이 1개 이상 있어야 개설 가능합니다. 원화 입출금 통장을 먼저 만들어주세요.");
            return "redirect:/mypage/account_open_main";
        }

    }



    @GetMapping("/en_account_open_1")
    public String en_account_open_1(Model model) {

        int termLocation = 6; // 6번: 외화통장개설 페이지

        List<TermsHistDTO> termsList = termsService.getTermsByLocation(termLocation);

        System.out.println("### termsList size = " + termsList.size());

        model.addAttribute("termsList", termsList);

        return "mypage/en_account_open_1";
    }

    @GetMapping("/en_account_open_2")
    public String en_account_open_2(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {

        // custInfoDTO 정보 보내기
        String custCode = userDetails.getUsername();
        CustInfoDTO custInfoDTO = mypageService.getCustInfo(custCode);
        model.addAttribute("custInfoDTO", custInfoDTO);

        // 계좌 개설 정보 전달용
        CustFrgnAcctDTO custFrgnAcctDTO = new CustFrgnAcctDTO();
        model.addAttribute("custFrgnAcctDTO", custFrgnAcctDTO);

        // 보안 등급 확인 후 1일, 1회 이체한도 보내기
        if(custInfoDTO.getCustSecurityLevel().equals(3)) {
            model.addAttribute("dayTrsfLmt", "7,000$");
            model.addAttribute("onceTrsfLmt", "7,000$");
        }else if(custInfoDTO.getCustSecurityLevel().equals(2)) {
            model.addAttribute("dayTrsfLmt", "35,000$");
            model.addAttribute("onceTrsfLmt", "7,000$");
        }else if(custInfoDTO.getCustSecurityLevel().equals(1)) {
            model.addAttribute("dayTrsfLmt", "350,000$");
            model.addAttribute("onceTrsfLmt", "70,000$");
        }

        return "mypage/en_account_open_2";
    }

    @PostMapping("/en_account_open_2")
    public String saveEnAcct(@ModelAttribute CustFrgnAcctDTO custFrgnAcctDTO, @AuthenticationPrincipal CustomUserDetails userDetails) {
        String userCode = userDetails.getUsername();
        CustInfoDTO custInfoDTO = mypageService.getCustInfo(userCode);
        custFrgnAcctDTO.setFrgnAcctCustCode(userCode);
        custFrgnAcctDTO.setFrgnAcctCustEngName(custInfoDTO.getCustEngName());

        log.info("custFrgnAcctDTO = " + custFrgnAcctDTO);

        mypageService.saveFrgnAcct(custFrgnAcctDTO);

        return "redirect:/mypage/en_account_open_3";
    }

    @GetMapping("/en_account_open_3")
    public String en_account_open_3() {

        return "mypage/en_account_open_3";
    }

    @PostMapping("/ko_transfer_1")
    public String ko_transfer_1(Model model, @RequestParam("acctNo") String acctNo) {

        // 계좌 정보 보내기
        CustAcctDTO custAcctDTO = mypageService.findCustAcct(acctNo);
        model.addAttribute("custAcctDTO", custAcctDTO);

        // 이체 정보 받을 객체 보내기
        CustTranHistDTO custTranHistDTO = new CustTranHistDTO();
            // 미리 설정할 거
        custTranHistDTO.setTranType(2); // 출금 : 2
        custTranHistDTO.setTranAcctNo(acctNo); // 계좌번호


        model.addAttribute("custTranHistDTO", custTranHistDTO);

        return  "mypage/ko_transfer_1";
    }

    @PostMapping("/ko_transfer_2")
    public String ko_transfer_2(Model model, @ModelAttribute CustTranHistDTO custTranHistDTO, @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("수정전 custTranHistDTO = " + custTranHistDTO);
        if(custTranHistDTO.getTranCustName() == null || custTranHistDTO.getTranCustName().trim().isEmpty()){
            custTranHistDTO.setTranCustName(userDetails.getCustName());
        }
        if(custTranHistDTO.getTranRecName() == null || custTranHistDTO.getTranRecName().trim().isEmpty()){
            custTranHistDTO.setTranRecName(custTranHistDTO.getTranRecAcctNo());
        }
        log.info("수정후 custTranHistDTO = " + custTranHistDTO);

        model.addAttribute("custTranHistDTO", custTranHistDTO);

        return  "mypage/ko_transfer_2";
    }

    @PostMapping("/ko_transfer_3")
    public String ko_transfer_3(@AuthenticationPrincipal CustomUserDetails userDetails, @ModelAttribute CustTranHistDTO custTranHistDTO, Model model) {
        // 전자서명 임시 승인 수정해야함
        custTranHistDTO.setTranEsignYn("Y");
        CustAcctDTO custAcctDTO = new  CustAcctDTO();
        log.info("마지막 단계(ko_transfer_3): custTranHistDTO = " + custTranHistDTO);

        if(custTranHistDTO.getTranEsignYn().equals("Y")) {
            // 이체 내역 db에 반영
            mypageService.modifyCustAcctBal(custTranHistDTO);
            model.addAttribute("custTranHistDTO", custTranHistDTO);
            custAcctDTO = mypageService.findCustAcct(custTranHistDTO.getTranAcctNo());
            model.addAttribute("custAcctDTO", custAcctDTO);
            model.addAttribute("state", "정상");
        }else {
            model.addAttribute("custTranHistDTO", custTranHistDTO);
            custAcctDTO = mypageService.findCustAcct(custTranHistDTO.getTranAcctNo());
            model.addAttribute("custAcctDTO", custAcctDTO);
            model.addAttribute("state", "실패");
        }

        return  "mypage/ko_transfer_3";
    }



}
