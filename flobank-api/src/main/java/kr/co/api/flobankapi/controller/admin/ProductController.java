package kr.co.api.flobankapi.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import kr.co.api.flobankapi.dto.ProductDTO;
import kr.co.api.flobankapi.dto.ProductLimitDTO;
import kr.co.api.flobankapi.service.admin.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/products")
public class ProductController {

    private final ProductService productService;

    @GetMapping("")
    public String productList(Model model) {
        model.addAttribute("activeItem", "products");
        return "admin/products";
    }

    @PostMapping("/register")
    public String registerProduct(
            ProductDTO dto,
            @RequestParam(value = "dpstCurrency", required = false) String[] currencies,
            @RequestParam("ageLimit") String ageLimit,
            @RequestParam(value="lmtCurrency", required = false) String[] lmtCurrency,
            @RequestParam(value="lmtMinAmt", required = false) Integer[] lmtMinAmt,
            @RequestParam(value="lmtMaxAmt", required = false) Integer[] lmtMaxAmt
    ) {

        // --- 통화 처리 ---
        if (currencies != null) {
            dto.setDpstCurrency(String.join(",", currencies));
        }

        // --- 나이제한 처리 ---
        if (ageLimit.equals("none")) {
            dto.setDpstMinAge(null);
            dto.setDpstMaxAge(null);
        }

        // --- 최소/최대 가입액 처리 ---
        List<ProductLimitDTO> limits = new ArrayList<>();

        if (lmtCurrency != null) {
            for (int i = 0; i < lmtCurrency.length; i++) {
                ProductLimitDTO limit = new ProductLimitDTO();
                limit.setLmtCurrency(lmtCurrency[i]);
                limit.setLmtMinAmt(lmtMinAmt[i]);
                limit.setLmtMaxAmt(lmtMaxAmt[i]);
                limits.add(limit);
            }
        }

        // --- 최소/최대 여부 Y/N 설정 ---
        boolean hasMin = limits.stream().anyMatch(l -> l.getLmtMinAmt() != null);
        boolean hasMax = limits.stream().anyMatch(l -> l.getLmtMaxAmt() != null);



        dto.setDpstMinYn(hasMin ? "Y" : "N");
        dto.setDpstMaxYn(hasMax ? "Y" : "N");

        dto.setDpstStatus(1);

        // --- 최종 저장 ---
        productService.insertProduct(dto, limits);

        return "redirect:/admin/products";
    }
}