package kr.co.api.flobankapi.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {

    /** 관리자 대시보드 */
    @GetMapping("/index")
    public String index(Model model) {
        model.addAttribute("activeItem", "dashboard");
        return "admin/index";
    }

    /** 고객센터 관리 */
    @GetMapping("/member")
    public String member(Model model) {
        model.addAttribute("activeItem", "member");
        return "admin/member";
    }

    /** 외화예금 관리 */
    @GetMapping("/products")
    public String products(Model model) {
        model.addAttribute("activeItem", "products");
        return "admin/products";
    }

    /** 외화예금 상세 보기 */
    @GetMapping("/products_view")
    public String productsView(Model model) {
        model.addAttribute("activeItem", "products");
        return "admin/products_view";
    }

    /** 환전 관리 */
    @GetMapping("/exchange")
    public String exchange(Model model) {
        model.addAttribute("activeItem", "exchange");
        return "admin/exchange";
    }

}
