package kr.co.api.flobankapi.controller;

import jakarta.servlet.http.HttpServletResponse;
import kr.co.api.flobankapi.dto.*;
import kr.co.api.flobankapi.jwt.CustomUserDetails;
import kr.co.api.flobankapi.service.DepositService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        model.addAttribute("dpstId", dpstId);
        model.addAttribute("activeItem","product");

        List<TermsHistDTO> termsList = depositService.getTerms();
        ProductDTO product = depositService.selectDpstProduct(dpstId);
        model.addAttribute("product", product);
        model.addAttribute("termsList",termsList);

        return "deposit/deposit_step1";
    }

    @Value("${file.upload.pdf-terms-path}")
    private String termsUploadPath;

    @GetMapping("/terms/download")
    public void downloadTerms(@RequestParam String thistTermOrder, @RequestParam String thistTermCate, HttpServletResponse response) throws IOException {

        log.info("termUploadPath : " + termsUploadPath);

        String termPath = depositService.getTermContent(thistTermOrder, thistTermCate).getThistFile();
        String fileName = Paths.get(termPath).getFileName().toString();
        String fullPath = termsUploadPath + "/" + fileName;
        log.info("fileName : " + fileName);
        log.info("fullPath : " + fullPath);

        Path path = Paths.get(fullPath);

        if (!Files.exists(path)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "파일을 찾을 수 없습니다.");
            return;
        }

        response.setContentType("application/pdf");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + fileName
        );
        response.setHeader("Content-Length", String.valueOf(Files.size(path)));

        // 4) 파일을 스트림으로 직접 내려보냄
        try (OutputStream os = response.getOutputStream()) {
            Files.copy(path, os);
            os.flush();
        }

    }

    @Value("${file.upload.pdf-products-path}")
    private String productsUploadPath;

    @GetMapping("/info/download")
    public void downloadDepositInfo(@RequestParam String dpstId, HttpServletResponse response) throws IOException {

        String termPath = depositService.getProduct(dpstId).getDpstInfoPdf();
        String fileName = Paths.get(termPath).getFileName().toString();
        String fullPath = productsUploadPath + "/" + fileName;

        Path path = Paths.get(fullPath);

        if (!Files.exists(path)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "파일을 찾을 수 없습니다.");
            return;
        }

        response.setContentType("application/pdf");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader(
                "Content-Disposition",
                "inline; filename=\"" + fileName + "\"; filename*=UTF-8''" + fileName
        );
        response.setHeader("Content-Length", String.valueOf(Files.size(path)));
        // 4) 파일을 스트림으로 직접 내려보냄
        try (OutputStream os = response.getOutputStream()) {
            Files.copy(path, os);
            os.flush();
        }
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

        if (dto.getAutoRenewYn() == null){
            dto.setAutoRenewYn("n");
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
        if (product.getDpstType() == 1){
            dpstAcctHdrDTO.setDpstHdrBalance(dto.getDpstAmount());
        }else {
            dpstAcctHdrDTO.setDpstHdrBalance(BigDecimal.valueOf(0));
        }
        dpstAcctHdrDTO.setDpstHdrInterest(dto.getAppliedInterest());
        if (product.getDpstType() == 1 && product.getDpstRateType()==1) {
            dpstAcctHdrDTO.setDpstHdrRate(dto.getAppliedRate());
        } else{
            dpstAcctHdrDTO.setDpstHdrRate(BigDecimal.valueOf(0));
        }

        // ==========================================
        // [수정 포인트] 상품 ID에 따른 분기 처리 시작
        // ==========================================

        // 특정 이벤트 상품 ID (예: FXD079) 인지 확인
        boolean isEventProduct = "FXD079".equals(dpstId);

        if (isEventProduct) {
            // [CASE 1] 이벤트 상품 (사전 신청)
            // 1. 상태를 '0'(사전신청)으로 설정
            dpstAcctHdrDTO.setDpstHdrStatus(0);

            // 2. 잔액은 0원 혹은 가입 금액으로 설정하되, 실제 출금은 안 함
            // (화면에 '가입금액'을 보여주려면 dto.getDpstAmount()를 넣고,
            // 실제 돈이 안 들어왔음을 표시하려면 0으로 넣으세요. 여기선 금액 정보 유지를 위해 값은 넣음)
            dpstAcctHdrDTO.setDpstHdrBalance(dto.getDpstAmount());

            // 3. 기존 로직 중 '입출금이 없는(Free)' 메서드 재활용 -> 돈 안 빠져나감
            DpstAcctHdrDTO insertDTO = depositService.openDepositFreeAcctTransaction(dpstAcctHdrDTO);
            model.addAttribute("insertDTO", insertDTO);

        } else {
            dpstAcctHdrDTO.setDpstHdrStatus(1);
            if (product.getDpstType() == 1) {
                dpstAcctHdrDTO.setDpstHdrCurrencyExp(dto.getDpstHdrCurrency());
            } else {
                dpstAcctHdrDTO.setDpstHdrCurrencyExp("KRW");
            }
            dpstAcctHdrDTO.setDpstHdrLinkedAcctNo(
                    "krw".equals(dto.getWithdrawType()) ? dto.getAcctNo() : dto.getBalNo()
            );
            dpstAcctHdrDTO.setDpstHdrExpAcctNo(
                    "krw".equals(dto.getWithdrawType()) ? dto.getAcctNo() : dto.getFrgnAcctNo()
            );

            dpstAcctHdrDTO.setDpstHdrAutoRenewYn("y".equals(dto.getAutoRenewYn()) ? "y" : "n");
            if ("y".equals(dto.getAutoRenewYn())) {
                dpstAcctHdrDTO.setDpstHdrAutoRenewTerm(dto.getAutoRenewTerm());
                dpstAcctHdrDTO.setDpstHdrAutoRenewCnt(0);
            }
            if ("krw".equals(dto.getWithdrawType()) || product.getDpstRateType() == 2) {
                dpstAcctHdrDTO.setDpstHdrLinkedAcctType(1);
            } else {
                dpstAcctHdrDTO.setDpstHdrLinkedAcctType(2);
            }
            dpstAcctHdrDTO.setDpstHdrPartWdrwCnt(0);
            dpstAcctHdrDTO.setDpstHdrInfoAgreeYn("y");
            dpstAcctHdrDTO.setDpstHdrInfoAgreeDt(LocalDateTime.now());



            // 첫 예금 거래 내역
            DpstAcctDtlDTO dtlDTO = new DpstAcctDtlDTO();
            if (product.getDpstType()==1){
                dtlDTO.setDpstDtlType(1);
                if (product.getDpstRateType() == 1){
                    dtlDTO.setDpstDtlAmount(dto.getDpstAmount());
                }else {
                    dtlDTO.setDpstDtlAmount(dto.getKrwAmount());
                }
                dtlDTO.setDpstDtlEsignYn("y");
                dtlDTO.setDpstDtlEsignDt(LocalDateTime.now());
                dtlDTO.setDpstDtlAppliedRate(dto.getAppliedRate());
            }




            // 내 계좌 거래내역
            CustTranHistDTO custTranHistDTO = new CustTranHistDTO();

            if (product.getDpstType()==1){
                custTranHistDTO.setTranCustName(user.getCustName());
                custTranHistDTO.setTranType(2);
                if (dto.getWithdrawType().equals("krw")) {
                    custTranHistDTO.setTranAmount(dto.getKrwAmount());
                    custTranHistDTO.setTranCurrency("KRW");
                }else {
                    custTranHistDTO.setTranAmount(dto.getDpstAmount());
                    custTranHistDTO.setTranCurrency(dpstAcctHdrDTO.getDpstHdrCurrency());
                }

                custTranHistDTO.setTranRecName(user.getCustName());
                custTranHistDTO.setTranRecBkCode("888");
                custTranHistDTO.setTranEsignYn("Y");
                DateTimeFormatter dt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
                custTranHistDTO.setTranEsignDt(LocalDateTime.now().format(dt));
            }


            // 트랜잭션 처리
            DpstAcctHdrDTO insertDTO;
            if (product.getDpstType()==1) {
                insertDTO = depositService.openDepositAcctTransaction(dpstAcctHdrDTO, dtlDTO, custTranHistDTO, dto.getWithdrawType());
            }else {
                insertDTO = depositService.openDepositFreeAcctTransaction(dpstAcctHdrDTO);
            }

            model.addAttribute("insertDTO", insertDTO);
        }
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
