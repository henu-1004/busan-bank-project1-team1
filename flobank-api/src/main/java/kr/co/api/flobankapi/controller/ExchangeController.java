package kr.co.api.flobankapi.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/exchange")
public class ExchangeController {
    @GetMapping("/benefit")
    public String benefit(Model model){
        model.addAttribute("activeItem","benefit");
        return "exchange/benefit";
    }

    @GetMapping("/info1")
    public String info1(Model model){
        model.addAttribute("activeItem","info1");
        return "exchange/info1";
    }

    @GetMapping("/info2")
    public String info2(Model model){
        model.addAttribute("activeItem","info2");
        return "exchange/info2";
    }

    @GetMapping("/info3")
    public String info3(Model model){
        model.addAttribute("activeItem","info3");
        return "exchange/info3";
    }

    @GetMapping("/step1")
    public String step1(){
        return "exchange/step1";
    }

    @GetMapping("/step2")
    public String step2(){
        return "exchange/step2";
    }

    @GetMapping("/step3")
    public String step3(){
        return "exchange/step3";
    }
}
