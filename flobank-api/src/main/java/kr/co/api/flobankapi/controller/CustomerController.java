package kr.co.api.flobankapi.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/customer")
public class CustomerController {
    @GetMapping("/event_list")
    public String event_list(Model model){
        model.addAttribute("activeItem","event");
        return "customer/event_list";
    }

    @GetMapping("/event_view")
    public String event_view(Model model){
        model.addAttribute("activeItem","event");
        return "customer/event_view";
    }

    @GetMapping("/faq_list")
    public String faq_list(Model model){
        model.addAttribute("activeItem","faq");
        return "customer/faq_list";
    }

    @GetMapping("/intro")
    public String intro(Model model){
        model.addAttribute("activeItem","intro");
        return "customer/intro";
    }

    @GetMapping("/notice_list")
    public String notice_list(Model model){
        model.addAttribute("activeItem","notice");
        return "customer/notice_list";
    }

    @GetMapping("/notice_view")
    public String notice_view(Model model){
        model.addAttribute("activeItem","notice");
        return "customer/notice_view";
    }

    @GetMapping("/qna_edit")
    public String qna_edit(Model model){
        model.addAttribute("activeItem","qna");
        return "customer/qna_edit";
    }

    @GetMapping("/qna_list")
    public String qna_list(Model model){
        model.addAttribute("activeItem","qna");
        return "customer/qna_list";
    }

    @GetMapping("/qna_view")
    public String qna_view(Model model){
        model.addAttribute("activeItem","qna");
        return "customer/qna_view";
    }

    @GetMapping("/qna_write")
    public String qna_write(Model model){
        model.addAttribute("activeItem","qna");
        return "customer/qna_write";
    }

}
