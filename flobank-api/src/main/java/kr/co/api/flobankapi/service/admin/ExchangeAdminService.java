package kr.co.api.flobankapi.service.admin;

import jakarta.annotation.PostConstruct;
import kr.co.api.flobankapi.dto.admin.exchange.CouponIssueDTO;
import kr.co.api.flobankapi.dto.admin.exchange.ExchangeHistoryDTO;
import kr.co.api.flobankapi.dto.admin.exchange.ExchangeStatsDTO;
import kr.co.api.flobankapi.mapper.admin.ExchangeAdminMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeAdminService {

    private final ExchangeAdminMapper exchangeAdminMapper;

    private ExchangeStatsDTO statsCache;

    @PostConstruct
    public void init() {
        refreshStats();
    }

    @Scheduled(fixedRate = 5 * 60 * 1000)
    public synchronized void refreshStats() {
        log.info("Refreshing exchange admin stats...");

        LocalDateTime baseTime = exchangeAdminMapper.selectStatsBaseTime();
        statsCache = ExchangeStatsDTO.builder()
                .currencyDailyAmounts(exchangeAdminMapper.selectDailyCurrencyAmounts())
                .dailyTotals(exchangeAdminMapper.selectDailyTotalAmounts())
                .lastUpdatedAt(baseTime != null ? baseTime : LocalDateTime.now())
                .build();
    }

    public ExchangeStatsDTO getStats() {
        if (statsCache == null) {
            refreshStats();
        }
        return statsCache;
    }

    public Map<String, Object> getExchangeHistoryPage(String searchType, String keyword, int page) {
        int pageSize = 10;

        int totalCount = exchangeAdminMapper.countExchangeHistory(searchType, keyword);
        int totalPage = (int) Math.ceil(totalCount / (double) pageSize);
        if (totalPage < 1) totalPage = 1;

        int currentPage = Math.min(Math.max(page, 1), totalPage);
        int start = (currentPage - 1) * pageSize + 1;
        int end = currentPage * pageSize;

        List<ExchangeHistoryDTO> list = exchangeAdminMapper.selectExchangeHistory(searchType, keyword, start, end);
        if (list == null) {
            list = Collections.emptyList();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("page", currentPage);
        result.put("pageSize", pageSize);
        result.put("totalPage", totalPage);
        result.put("totalCount", totalCount);
        return result;
    }

    public Map<String, Object> getCouponIssuePage(int page) {
        int pageSize = 5;

        int totalCount = exchangeAdminMapper.countCouponIssues();
        int totalPage = (int) Math.ceil(totalCount / (double) pageSize);
        if (totalPage < 1) totalPage = 1;

        int currentPage = Math.min(Math.max(page, 1), totalPage);
        int start = (currentPage - 1) * pageSize + 1;
        int end = currentPage * pageSize;

        List<CouponIssueDTO> list = exchangeAdminMapper.selectCouponIssues(start, end);
        if (list == null) {
            list = Collections.emptyList();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("page", currentPage);
        result.put("pageSize", pageSize);
        result.put("totalPage", totalPage);
        result.put("totalCount", totalCount);
        return result;
    }
}