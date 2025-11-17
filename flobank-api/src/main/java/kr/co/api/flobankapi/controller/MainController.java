package kr.co.api.flobankapi.controller;

import kr.co.api.flobankapi.service.BriefingService;
import kr.co.api.flobankapi.dto.BriefingViewDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class MainController {

    /**
     * 메인 페이지
     */
    private final BriefingService briefingService;
    @GetMapping("/")
    public String mainPage(
            @RequestParam(value = "mode", required = false, defaultValue = "oneday")
            String mode,
            Model model
    ) {

        BriefingViewDTO briefingView = briefingService.buildBriefingView(mode);

        model.addAttribute("briefingMode", briefingView.getBriefingMode());
        model.addAttribute("briefingTitle", briefingView.getBriefingTitle());
        model.addAttribute("briefingDateText", briefingView.getBriefingDateText());
        model.addAttribute("briefingLines", briefingView.getBriefingLines());
        return "index";
    }

}