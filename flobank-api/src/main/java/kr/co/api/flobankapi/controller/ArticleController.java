package kr.co.api.flobankapi.controller;

import kr.co.api.flobankapi.dto.BriefingViewDTO;
import kr.co.api.flobankapi.service.ArticleService;
import kr.co.api.flobankapi.service.BriefingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;
    private final BriefingService briefingService;

    @GetMapping("/articles")
    public String articleList(
            @RequestParam(value = "mode", required = false, defaultValue = "oneday") String mode,
            @RequestParam(value = "page", defaultValue = "1") int page,
            Model model
    ) {

        BriefingViewDTO briefingView = briefingService.buildBriefingView(mode);
        Map<String, Object> articlePage = articleService.getArticlePage(mode, page);

        model.addAttribute("briefingMode", briefingView.getBriefingMode());
        model.addAttribute("briefingTitle", briefingView.getBriefingTitle());
        model.addAttribute("briefingDateText", briefingView.getBriefingDateText());
        model.addAttribute("briefingLines", briefingView.getBriefingLines());

        model.addAttribute("articles", articlePage.get("list"));
        model.addAttribute("page", articlePage.get("page"));
        model.addAttribute("totalPage", articlePage.get("totalPage"));
        model.addAttribute("totalCount", articlePage.get("totalCount"));
        model.addAttribute("pageSize", articlePage.get("pageSize"));

        return "article/list";
    }
}
