package kr.co.api.flobankapi.controller;



import kr.co.api.flobankapi.dto.*;
import kr.co.api.flobankapi.service.TermsDbService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/termsdb")
@RequiredArgsConstructor
public class AdminTermsDbController {

    private final TermsDbService service;

    /** 목록 */
    @GetMapping
    public String list(Model model) {
        model.addAttribute("termsList", service.getAllTerms());
        model.addAttribute("activeItem", "terms");
        return "admin/termsdb/terms_list";
    }

    /** 등록 페이지 */
    @GetMapping("/register")
    public String registerPage() {
        return "admin/termsdb/terms_register";
    }

    /** 등록 처리 */
    @PostMapping("/register")
    public String register(@RequestParam int cate,
                           @RequestParam String title,
                           @RequestParam String content,
                           RedirectAttributes ra) {

        service.createTerms(cate, title, content, "admin");
        ra.addFlashAttribute("msg", "약관이 등록되었습니다!");

        return "redirect:/admin/termsdb";
    }

    /** 수정 페이지 */
    @GetMapping("/edit")
    public String edit(@RequestParam int cate,
                       @RequestParam int order,
                       Model model) {

        TermsHistDTO latest = service.getLatestHist(cate, order);

        model.addAttribute("latest", latest);
        model.addAttribute("cate", cate);
        model.addAttribute("order", order);

        return "admin/termsdb/terms_edit";
    }

    /** 수정 처리 */
    @PostMapping("/update")
    public String update(@RequestParam int cate,
                         @RequestParam int order,
                         @RequestParam String title,
                         @RequestParam String content,
                         @RequestParam int currentVersion,
                         RedirectAttributes ra) {

        service.updateTerms(cate, order, title, content, currentVersion, "admin");
        ra.addFlashAttribute("msg", "약관이 수정되었습니다!");

        return "redirect:/admin/termsdb";
    }

    /** 삭제 */
    @GetMapping("/delete")
    public String delete(@RequestParam int cate,
                         @RequestParam int order,
                         RedirectAttributes ra) {

        service.deleteTerms(cate, order);
        ra.addFlashAttribute("msg", "약관이 삭제되었습니다!");

        return "redirect:/admin/termsdb";
    }
}
