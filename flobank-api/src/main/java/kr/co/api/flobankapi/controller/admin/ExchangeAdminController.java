package kr.co.api.flobankapi.controller.admin;

import kr.co.api.flobankapi.dto.admin.exchange.ExchangeStatsDTO;
import kr.co.api.flobankapi.service.admin.ExchangeAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.Map;

@Controller
@RequestMapping("/admin/exchange")
@RequiredArgsConstructor
public class ExchangeAdminController {

    private final ExchangeAdminService exchangeAdminService;

    @GetMapping
    public String exchange(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(name = "couponPage", defaultValue = "1") int couponPage,
                           @RequestParam(name = "searchType", required = false) String searchType,
                           @RequestParam(name = "keyword", required = false) String keyword,
                           Model model) {

        ExchangeStatsDTO stats = exchangeAdminService.getStats();
        LocalDateTime baseTime = stats != null ? stats.getLastUpdatedAt() : null;

        Map<String, Object> exchangeHistory = exchangeAdminService.getExchangeHistoryPage(searchType, keyword, page);
        Map<String, Object> couponPageData = exchangeAdminService.getCouponIssuePage(couponPage);

        model.addAttribute("stats", stats);
        model.addAttribute("baseTime", baseTime);

        model.addAttribute("exchangeList", exchangeHistory.get("list"));
        model.addAttribute("exchangePage", exchangeHistory.get("page"));
        model.addAttribute("exchangePageSize", exchangeHistory.get("pageSize"));
        model.addAttribute("exchangeTotalPage", exchangeHistory.get("totalPage"));
        model.addAttribute("exchangeTotalCount", exchangeHistory.get("totalCount"));

        model.addAttribute("couponList", couponPageData.get("list"));
        model.addAttribute("couponPage", couponPageData.get("page"));
        model.addAttribute("couponPageSize", couponPageData.get("pageSize"));
        model.addAttribute("couponTotalPage", couponPageData.get("totalPage"));
        model.addAttribute("couponTotalCount", couponPageData.get("totalCount"));

        model.addAttribute("searchType", searchType);
        model.addAttribute("keyword", keyword);

        model.addAttribute("activeItem", "exchange");
        return "admin/exchange";
    }
}
