package kr.co.api.flobankapi.controller.admin;

import kr.co.api.flobankapi.dto.admin.exchange.ExchangeStatsDTO;
import kr.co.api.flobankapi.service.admin.ExchangeAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.util.Map;

@Controller
@RequestMapping("/admin/exchange")
@RequiredArgsConstructor
public class ExchangeAdminController {

    private final ExchangeAdminService exchangeAdminService;

    /**
     * 관리자 > 환전 관리 메인 화면
     *
     * 기능 포함:
     * 1) 일자별 통화별 환전금액 그래프 데이터
     * 2) 일자별 총 환전액 그래프 데이터
     * 3) 환전 이력(검색 + 페이징)
     * 4) 환전 수수료 쿠폰 발급 현황(페이징)
     *
     * @param page        환전 이력 페이지 번호
     * @param couponPage  쿠폰 발급 현황 페이지 번호
     * @param searchType  검색 타입 (customer / exchange)
     * @param keyword     검색어 (고객번호 또는 환전번호)
     * @param model       화면 전달 객체
     */

    @GetMapping
    public String exchange(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(name = "couponPage", defaultValue = "1") int couponPage,
                           @RequestParam(name = "searchType", required = false) String searchType,
                           @RequestParam(name = "keyword", required = false) String keyword,
                           Model model) {

        // 검색어가 null 또는 문자열 "null" 로 들어오면 공백 처리
        if (keyword == null || "null".equalsIgnoreCase(keyword)) {
            keyword = "";
        }

        // 1) 환전 통계(그래프 데이터 + 갱신 시간)
        ExchangeStatsDTO stats = exchangeAdminService.getStats();
        LocalDateTime baseTime = stats != null ? stats.getLastUpdatedAt() : null;

        // 2) 환전 이력 페이징 + 검색 처리
        Map<String, Object> exchangeHistory = exchangeAdminService.getExchangeHistoryPage(searchType, keyword, page);

        // 3) 환전 수수료 쿠폰 발급 현황 페이징
        Map<String, Object> couponPageData = exchangeAdminService.getCouponIssuePage(couponPage);

        // ============  Model 데이터 바인딩 ============

        // 통계 그래프
        model.addAttribute("stats", stats);
        model.addAttribute("baseTime", LocalDateTime.now());

        // 환전 이력
        model.addAttribute("exchangeList", exchangeHistory.get("list"));
        model.addAttribute("exchangePage", exchangeHistory.get("page"));
        model.addAttribute("exchangePageSize", exchangeHistory.get("pageSize"));
        model.addAttribute("exchangeTotalPage", exchangeHistory.get("totalPage"));
        model.addAttribute("exchangeTotalCount", exchangeHistory.get("totalCount"));

        // 쿠폰 발급 현황
        model.addAttribute("couponList", couponPageData.get("list"));
        model.addAttribute("couponPage", couponPageData.get("page"));
        model.addAttribute("couponPageSize", couponPageData.get("pageSize"));
        model.addAttribute("couponTotalPage", couponPageData.get("totalPage"));
        model.addAttribute("couponTotalCount", couponPageData.get("totalCount"));

        // 검색 조건 유지
        model.addAttribute("searchType", searchType);
        model.addAttribute("keyword", keyword);

        // 사이드바 메뉴 활성화 표시용
        model.addAttribute("activeItem", "exchange");

        // admin/exchange.html 렌더링
        return "admin/exchange";
    }

    /**
     * AJAX 요청으로 최신 환전 통계만 가져오는 엔드포인트
     * (5분 캐싱된 데이터 반환)
     *
     * @return ExchangeStatsDTO (통화별 일자별 금액 + 전체 일자별 환전금액)
     */
    @GetMapping("/stats")
    @ResponseBody
    public ExchangeStatsDTO getStats(
            @RequestParam(value = "date", required = false) String date
    ) {
        if (date != null && !date.isEmpty()) {
            return exchangeAdminService.getExchangeStatsByDate(date);
        }
        return exchangeAdminService.getStats();
    }






}