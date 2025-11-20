package kr.co.api.flobankapi.controller;

import kr.co.api.flobankapi.dto.ProductDTO;
import kr.co.api.flobankapi.service.DepositService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/deposit")
public class DepositController {

    private final DepositService depositService;

    public DepositController(DepositService depositService) {
        this.depositService = depositService;
    }

    @GetMapping("/deposit_step1")
    public String deposit_step1(Model model){
        model.addAttribute("activeItem","product");
        return "deposit/deposit_step1";
    }

    @GetMapping("/deposit_step2")
    public String deposit_step2(Model model){
        model.addAttribute("activeItem","product");

        return "deposit/deposit_step2";
    }
    @GetMapping("/deposit_step3")
    public String deposit_step3(Model model){
        model.addAttribute("activeItem","product");
        return "deposit/deposit_step3";
    }

    @GetMapping("/deposit_step4")
    public String deposit_step4(Model model){
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



}
