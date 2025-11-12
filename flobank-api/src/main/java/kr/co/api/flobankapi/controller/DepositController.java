package kr.co.api.flobankapi.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/deposit")
public class DepositController {

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
        model.addAttribute("activeItem","product");
        return "deposit/list";
    }

    @GetMapping("/view")
    public String view(Model model){
        model.addAttribute("activeItem","product");
        return "deposit/view";
    }

}
