package kr.co.api.flobankapi.controller;

import kr.co.api.flobankapi.dto.BriefingDTO;
import kr.co.api.flobankapi.service.BriefingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

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

        BriefingDTO latest = briefingService.getLatestBrifing(mode);

        // 최신 데이터가 있는 경우만 처리
        if (latest != null) {

            if (latest.getContent() != null) {
                String[] lines = latest.getContent().split("\\n");
                model.addAttribute("briefingLines", lines);
            }

            if (mode.equals("oneday")) {

                LocalDate date = null;
                if (latest.getBriefingDate() != null) {
                    date = latest.getBriefingDate().toLocalDate();
                }

                model.addAttribute("briefingTitle", "오늘의 브리핑");
                model.addAttribute("briefingDateText",
                        date != null ? date.toString() : "날짜 없음");
            }
        }

        // recent5 모드는 최신 브리핑 없어도 항상 계산 가능
        if (mode.equals("recent5")) {
            LocalDate today = LocalDate.now();
            LocalDate fiveDaysAgo = today.minusDays(5);

            model.addAttribute("briefingTitle", "최근 5일 브리핑");
            model.addAttribute("briefingDateText",
                    fiveDaysAgo + " ~ " + today);
        }

        model.addAttribute("briefingMode", mode);
        return "index";
    }

}