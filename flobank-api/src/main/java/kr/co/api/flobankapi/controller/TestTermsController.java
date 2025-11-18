package kr.co.api.flobankapi.controller;

import kr.co.api.flobankapi.dto.TermsHistDTO;
import kr.co.api.flobankapi.dto.TermsMasterDTO;
import kr.co.api.flobankapi.service.TermsDbService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class TestTermsController {

    private final TermsDbService termsDbService;

    /** 전체 리스트 페이지 */
    @GetMapping("/test/terms")
    public String testTerms(Model model) {
        List<TermsMasterDTO> termsList = termsDbService.getAllTerms();
        model.addAttribute("termsList", termsList);
        return "test/terms_list";
    }

    /** (기존) 상세보기 페이지 이동용 */
    @GetMapping("/test/terms/detail")
    public String testTermsDetail(int cate, int order, Model model) {
        TermsHistDTO hist = termsDbService.getLatestHist(cate, order);
        model.addAttribute("hist", hist);
        return "test/terms_detail";
    }

    /** (신규) AJAX JSON 응답 */
    @GetMapping("/test/terms/detail/json")
    @ResponseBody
    public TermsHistDTO testTermsDetailJson(int cate, int order) {
        return termsDbService.getLatestHist(cate, order);
    }
}
