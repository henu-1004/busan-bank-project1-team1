package kr.co.api.flobankapi.controller;

import kr.co.api.flobankapi.dto.*;
import kr.co.api.flobankapi.jwt.CustomUserDetails;
import kr.co.api.flobankapi.service.DepositService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.util.Date;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.util.HashMap;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/deposit")
public class DepositController {
    private final DepositService depositService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/deposit_step1")
    public String deposit_step1(Model model, @RequestParam String dpstId) {
        model.addAttribute("activeItem","product");
        return "deposit/deposit_step1";
    }

    @GetMapping("/deposit_step2")
    public String deposit_step2(Model model, @RequestParam String dpstId, @AuthenticationPrincipal CustomUserDetails user){
        model.addAttribute("activeItem","product");

        ProductDTO product = depositService.selectDpstProduct(dpstId);
        model.addAttribute("product",product);
        List<CustAcctDTO> accounts = depositService.getAcctList(user.getUsername());
        model.addAttribute("accounts",accounts);
        CustFrgnAcctDTO frgnAccount = depositService.getFrgnAcct(user.getUsername());
        model.addAttribute("frgnAccount",frgnAccount);
        List<FrgnAcctBalanceDTO> frgnAccountBals = depositService.getFrgnAcctBalList(frgnAccount.getFrgnAcctNo());
        model.addAttribute("frgnAccountBals",frgnAccountBals);

        Map<String, Integer> limitMinMap = new HashMap<>();
        Map<String, Integer> limitMaxMap = new HashMap<>();

        if (product.getLimits() != null) {
            limitMinMap = product.getLimits().stream().collect(Collectors.toMap(ProductLimitDTO::getLmtCurrency, ProductLimitDTO::getLmtMinAmt));
            limitMaxMap = product.getLimits().stream().collect(Collectors.toMap(ProductLimitDTO::getLmtCurrency, ProductLimitDTO::getLmtMaxAmt));
            log.info("limitMinMap:{}",limitMinMap);
            log.info("limitMaxMap:{}",limitMaxMap);
        }

        model.addAttribute("limitMinMap",limitMinMap);
        model.addAttribute("limitMaxMap",limitMaxMap);

        return "deposit/deposit_step2";
    }

    @PostMapping("/calc")
    @ResponseBody
    public DepositExchangeDTO calc(@RequestBody Map<String, String> req) {
        System.out.println("⚡ POST /deposit/calc 호출됨!");

        String currency = req.get("currency");
        DepositExchangeDTO exDTO = depositService.exchangeCalc(currency);
        BigDecimal bdAmt = new BigDecimal(req.get("amount"));
        BigDecimal krwAmt = bdAmt.multiply(exDTO.getAppliedRate())
                .setScale(0, RoundingMode.FLOOR);

        exDTO.setKrwAmount(krwAmt);


        return exDTO;
    }

    @GetMapping("/deposit_step3")
    public String deposit_step3(Model model, @RequestParam String dpstId){
        model.addAttribute("activeItem","product");
        return "deposit/deposit_step3";
    }

    @PostMapping("/deposit_step3")
    public String depositStep3(DepositRequestDTO dto, @RequestParam String dpstId, Model model,  @AuthenticationPrincipal CustomUserDetails user) {

        boolean isValidPw = false;
        String pw = "";
        if (dto.getWithdrawType().equals("krw")){
            pw = depositService.getKAcctPw(dto.getAcctNo());
            isValidPw = passwordEncoder.matches(dto.getAcctPw(), pw);
        }else{
            pw = depositService.getFAcctPw(dto.getFrgnAcctNo());
            isValidPw = passwordEncoder.matches(dto.getFrgnAcctPw(), pw);
        }

        ProductDTO product = depositService.selectDpstProduct(dpstId);
        model.addAttribute("product",product);
        List<CustAcctDTO> accounts = depositService.getAcctList(user.getUsername());
        model.addAttribute("accounts",accounts);
        CustFrgnAcctDTO frgnAccount = depositService.getFrgnAcct(user.getUsername());
        model.addAttribute("frgnAccount",frgnAccount);
        List<FrgnAcctBalanceDTO> frgnAccountBals = depositService.getFrgnAcctBalList(frgnAccount.getFrgnAcctNo());
        model.addAttribute("frgnAccountBals",frgnAccountBals);


        Map<String, Integer> limitMinMap = new HashMap<>();
        Map<String, Integer> limitMaxMap = new HashMap<>();

        if (product.getLimits() != null) {
            limitMinMap = product.getLimits().stream().collect(Collectors.toMap(ProductLimitDTO::getLmtCurrency, ProductLimitDTO::getLmtMinAmt));
            limitMaxMap = product.getLimits().stream().collect(Collectors.toMap(ProductLimitDTO::getLmtCurrency, ProductLimitDTO::getLmtMaxAmt));
            log.info("limitMinMap:{}",limitMinMap);
            log.info("limitMaxMap:{}",limitMaxMap);
        }

        model.addAttribute("limitMinMap",limitMinMap);
        model.addAttribute("limitMaxMap",limitMaxMap);

        if (!isValidPw) {
            model.addAttribute("errorPw", "비밀번호가 일치하지 않습니다.");
            model.addAttribute("activeItem","product");


            return "deposit/deposit_step2";
        }

        LocalDate maturityDate = LocalDate.now().plusMonths(dto.getDpstHdrMonth());
        model.addAttribute("maturityDate",maturityDate); //만기일

        InterestRateDTO interestRateDTO = depositService.getRecentInterest(dto.getDpstHdrCurrency());
        BigDecimal appliedInterest;
        switch (dto.getDpstHdrMonth()) {
            case 1:
                appliedInterest = interestRateDTO.getRate1M();
                break;
            case 2:
                appliedInterest = interestRateDTO.getRate2M();
                break;
            case 3:
                appliedInterest = interestRateDTO.getRate3M();
                break;
            case 4:
                appliedInterest = interestRateDTO.getRate4M();
                break;
            case 5:
                appliedInterest = interestRateDTO.getRate5M();
                break;
            case 6:
                appliedInterest = interestRateDTO.getRate6M();
                break;
            case 7:
                appliedInterest = interestRateDTO.getRate7M();
                break;
            case 8:
                appliedInterest = interestRateDTO.getRate8M();
                break;
            case 9:
                appliedInterest = interestRateDTO.getRate9M();
                break;
            case 10:
                appliedInterest = interestRateDTO.getRate10M();
                break;
            case 11:
                appliedInterest = interestRateDTO.getRate11M();
                break;
            default:
                appliedInterest = interestRateDTO.getRate12M();
                break;
        }
        model.addAttribute("appliedInterest",appliedInterest);

        model.addAttribute("dto", dto);
        model.addAttribute("dpstId", dpstId);



        return "deposit/deposit_step3";
    }

    @GetMapping("/deposit_step4")
    public String deposit_step4(Model model, @RequestParam String dpstId){
        model.addAttribute("activeItem","product");
        return "deposit/deposit_step4";
    }

    @PostMapping("/deposit_step4")
    public String depositStep4(DepositRequestDTO dto, @RequestParam String dpstId, Model model,  @AuthenticationPrincipal CustomUserDetails user) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        ProductDTO product = depositService.selectDpstProduct(dpstId);
        model.addAttribute("product",product);
        List<CustAcctDTO> accounts = depositService.getAcctList(user.getUsername());
        model.addAttribute("accounts",accounts);
        CustFrgnAcctDTO frgnAccount = depositService.getFrgnAcct(user.getUsername());
        model.addAttribute("frgnAccount",frgnAccount);
        List<FrgnAcctBalanceDTO> frgnAccountBals = depositService.getFrgnAcctBalList(frgnAccount.getFrgnAcctNo());
        model.addAttribute("frgnAccountBals",frgnAccountBals);
        model.addAttribute("custName", user.getCustName());


        model.addAttribute("dto", dto);
        model.addAttribute("dpstId", dpstId);

        DpstAcctHdrDTO dpstAcctHdrDTO = new DpstAcctHdrDTO();
        dpstAcctHdrDTO.setDpstHdrDpstId(dpstId);
        dpstAcctHdrDTO.setDpstHdrPw(passwordEncoder.encode(dto.getDpstPw()));
        dpstAcctHdrDTO.setDpstHdrCustCode(user.getUsername());
        dpstAcctHdrDTO.setDpstHdrMonth(dto.getDpstHdrMonth());
        dpstAcctHdrDTO.setDpstHdrStartDy(LocalDate.now().format(formatter));
        dpstAcctHdrDTO.setDpstHdrFinDy(LocalDate.now().plusMonths(dto.getDpstHdrMonth()).format(formatter));
        dpstAcctHdrDTO.setDpstHdrCurrency(dto.getDpstHdrCurrency());
        dpstAcctHdrDTO.setDpstHdrBalance(dto.getDpstAmount());
        dpstAcctHdrDTO.setDpstHdrInterest(dto.getAppliedInterest());
        dpstAcctHdrDTO.setDpstHdrStatus(1);
        dpstAcctHdrDTO.setDpstHdrLinkedAcctNo(
                "krw".equals(dto.getWithdrawType()) ? dto.getAcctNo() : dto.getFrgnAcctNo()
        );
        dpstAcctHdrDTO.setDpstHdrAutoRenewYn("y".equals(dto.getAutoRenewYn()) ? "y" : "n");
        if ("y".equals(dto.getAutoRenewYn())) {
            dpstAcctHdrDTO.setDpstHdrAutoRenewTerm(dto.getAutoRenewTerm());
            dpstAcctHdrDTO.setDpstHdrAutoRenewCnt(0);
        }
        dpstAcctHdrDTO.setDpstHdrPartWdrwCnt(0);
        dpstAcctHdrDTO.setDpstHdrInfoAgreeYn("Y");
        dpstAcctHdrDTO.setDpstHdrInfoAgreeDt(LocalDateTime.now());

        return "deposit/deposit_step4";
    }

    @GetMapping("/info")
    public String info(Model model){
        model.addAttribute("activeItem","info");
        return "deposit/info";
    }

    @GetMapping("/list")
    public String list(Model model){
        model.addAttribute("activeItem", "product");

        List<ProductDTO> list = depositService.getActiveProducts();
        int count = depositService.getActiveProductCount();
        model.addAttribute("activeItem", "product");

        model.addAttribute("list", list);
        model.addAttribute("count", count);

        return "deposit/list";
    }

    @GetMapping("/view")
    public String view(@RequestParam("dpstId") String dpstId, Model model) {
        ProductDTO product = depositService.getProduct(dpstId);

        String termsFilePath = depositService.getTermsFileByTitle(product.getDpstName());

        LocalDate delibDate = LocalDate.parse(product.getDpstDelibDy(), DateTimeFormatter.ofPattern("yyyyMMdd"));
        LocalDate startDate = LocalDate.parse(product.getDpstDelibStartDy(), DateTimeFormatter.ofPattern("yyyyMMdd"));
        model.addAttribute("product", product);
        model.addAttribute("activeItem", "product");
        model.addAttribute("delibDate", delibDate);
        model.addAttribute("startDate", startDate);
        model.addAttribute("termsFilePath", termsFilePath);

        return "deposit/view";
    }

    @GetMapping("/rates")
    @ResponseBody
    public List<DepositRateDTO> getExchangeRates(
            @RequestParam("baseDate") @DateTimeFormat(pattern = "yyyy-MM-dd") Date baseDate) {

        List<DepositRateDTO> rates = depositService.getRatesByBaseDate(baseDate);

        return rates;
    }

}
