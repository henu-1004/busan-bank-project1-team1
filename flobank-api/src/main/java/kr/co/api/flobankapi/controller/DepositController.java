package kr.co.api.flobankapi.controller;

import kr.co.api.flobankapi.dto.*;
import kr.co.api.flobankapi.jwt.CustomUserDetails;
import kr.co.api.flobankapi.service.DepositService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
    @GetMapping("/deposit_step3")
    public String deposit_step3(Model model, @RequestParam String dpstId){
        model.addAttribute("activeItem","product");
        return "deposit/deposit_step3";
    }

    @GetMapping("/deposit_step4")
    public String deposit_step4(Model model, @RequestParam String dpstId){
        model.addAttribute("activeItem","product");
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

        model.addAttribute("list", list);
        model.addAttribute("count", count);

        return "deposit/list";
    }

    @GetMapping("/view")
    public String view(@RequestParam("dpstId") String dpstId, Model model) {
        ProductDTO product = depositService.getProduct(dpstId);
        LocalDate delibDate = LocalDate.parse(product.getDpstDelibDy(), DateTimeFormatter.ofPattern("yyyyMMdd"));
        LocalDate startDate = LocalDate.parse(product.getDpstDelibStartDy(), DateTimeFormatter.ofPattern("yyyyMMdd"));
        model.addAttribute("product", product);
        model.addAttribute("delibDate", delibDate);
        model.addAttribute("startDate", startDate);

        return "deposit/view";
    }

}
