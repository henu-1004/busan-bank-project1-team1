package kr.co.api.flobankapi.controller;

import kr.co.api.flobankapi.dto.TermsHistDTO;
import kr.co.api.flobankapi.service.TermsDbService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/terms")
@RequiredArgsConstructor
public class AdminTermsDbController {

    private final TermsDbService service;

    /** 약관 목록 */
    @GetMapping
    public String list(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(required = false) String type,
                       @RequestParam(required = false) String keyword,
                       Model model) {

        int pageSize = 3;

        Map<String, Object> result = service.getTermsPage(page, pageSize, type, keyword);

        model.addAttribute("termsList", result.get("list"));
        model.addAttribute("totalPage", result.get("totalPage"));
        model.addAttribute("page", page);

        model.addAttribute("type", type);
        model.addAttribute("keyword", keyword);
        model.addAttribute("activeItem", "terms");

        return "admin/terms";
    }


    /** 약관 상세(JSON) */
    @GetMapping("/detail")
    @ResponseBody
    public Map<String, Object> detail(@RequestParam int cate,
                                      @RequestParam int order) {

        return service.getTermsDetail(cate, order);
    }


    /** 등록 처리 */
    @PostMapping("/register")
    public String register(@RequestParam int cate,
                           @RequestParam String title,
                           @RequestParam String content,
                           @RequestParam(required = false) MultipartFile file,
                           RedirectAttributes ra) {

        try {
            service.createTerms(cate, title, content, "admin", file);
            ra.addFlashAttribute("msg", "약관이 등록되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("msg", "약관 등록 실패: " + e.getMessage());
        }

        return "redirect:/admin/terms";
    }

    /** 수정 처리 */
    @PostMapping("/update")
    @ResponseBody
    public Map<String, Object> update(@RequestParam int cate,
                                      @RequestParam int order,
                                      @RequestParam String title,
                                      @RequestParam String content,
                                      @RequestParam int currentVersion,
                                      @RequestParam(required = false) String verMemo) {

        Map<String, Object> result = new HashMap<>();

        try {
            service.updateTerms(cate, order, title, content, currentVersion, "admin", verMemo);
            result.put("status", "OK");
        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("message", e.getMessage());
        }

        return result;
    }






}
