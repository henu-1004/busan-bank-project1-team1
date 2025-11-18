package kr.co.api.flobankapi.controller;

import kr.co.api.flobankapi.dto.TermsHistDTO;
import kr.co.api.flobankapi.dto.TermsMasterDTO;
import kr.co.api.flobankapi.service.TermsDbService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class TestTermsController {

    private final TermsDbService termsDbService;

    /** 테스트용 약관 전체 조회 페이지 */
    @GetMapping("/test/terms")
    public String testTerms(Model model) {

        // ★ 수정 포인트: getAllTermsWithLatestVersion() → getAllTerms()
        List<TermsMasterDTO> termsList = termsDbService.getAllTerms();

        model.addAttribute("termsList", termsList);
        return "test/terms_list";  // templates/test/terms_list.html
    }

    /** 특정 약관 상세보기 (최신 버전) */
    @GetMapping("/test/terms/detail")
    public String testTermsDetail(int cate, int order, Model model) {

        TermsHistDTO hist = termsDbService.getLatestHist(cate, order);

        model.addAttribute("hist", hist);
        return "test/terms_detail";  // templates/test/terms_detail.html
    }
}
