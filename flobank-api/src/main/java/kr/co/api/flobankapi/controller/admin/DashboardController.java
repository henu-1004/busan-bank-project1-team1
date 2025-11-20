package kr.co.api.flobankapi.controller.admin;


import kr.co.api.flobankapi.dto.admin.dashboard.DashboardDTO;
import kr.co.api.flobankapi.service.admin.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    // /admin → 대시보드 메인
    @GetMapping({"", "/", "/index"})
    public String adminHome(Model model) {

        DashboardDTO stats = dashboardService.getStats();
        model.addAttribute("stats", stats);
        model.addAttribute("baseTime", LocalDateTime.now());
        model.addAttribute("activeItem", "dashboard");
        return "admin/index";
    }
}
