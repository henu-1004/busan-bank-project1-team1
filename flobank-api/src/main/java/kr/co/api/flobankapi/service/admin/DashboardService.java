package kr.co.api.flobankapi.service.admin;

import jakarta.annotation.PostConstruct;
import kr.co.api.flobankapi.dto.admin.dashboard.*;
import kr.co.api.flobankapi.mapper.BoardMapper;
import kr.co.api.flobankapi.mapper.admin.DashboardMapper;
import kr.co.api.flobankapi.mapper.admin.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final DashboardMapper dashboardMapper;

    private DashboardDTO cache;
    private LocalDateTime lastUpdatedAt;

    @PostConstruct
    public void init() {
        refreshStats();
    }

    // @EnableScheduling 설정 클래스에 꼭 붙여야 동작함
    @Scheduled(fixedRate = 5 * 60 * 1000) // 5분마다
    public void refreshStats() {
        log.info("Refreshing dashboard stats...");

        // 1) 거래 건수
        int wonCount  = dashboardMapper.selectTodayWonTxCount();
        int remtCount = dashboardMapper.selectTodayFrgnRemtTxCount();
        int exchCount = 0; // TODO: 환전 테이블 생기면 Mapper 추가

        List<TxCountDTO> txCounts = List.of(
                TxCountDTO.builder().type("입출금").count(wonCount).build(),
                TxCountDTO.builder().type("환전").count(exchCount).build(),
                TxCountDTO.builder().type("외화송금").count(remtCount).build()
        );

        // 2) 가입자 수
        List<JoinStatsDTO> dailyJoin   = dashboardMapper.selectDailyJoinStats();
//        List<JoinStatsDTO> weeklyJoin  = dashboardMapper.selectWeeklyJoinStats();
//        List<JoinStatsDTO> monthlyJoin = dashboardMapper.selectMonthlyJoinStats();

        // 3) 연령/성별
        List<AgeBandDTO> ageDist   = dashboardMapper.selectAgeDist();
        List<GenderStatsDTO> genderDist = dashboardMapper.selectGenderDist();

        // 4) 방문자 수 (나중에 구현)
        int todayVisitCount    = 0;
        int recent5mVisitCount = 0;

        this.lastUpdatedAt = LocalDateTime.now();

        DashboardDTO dto = DashboardDTO.builder()
                .todayVisitCount(todayVisitCount)
                .recent5mVisitCount(recent5mVisitCount)
                .todayTxCounts(txCounts)
                .dailyJoin(dailyJoin)
//                .weeklyJoin(weeklyJoin)
//                .monthlyJoin(monthlyJoin)
                .ageDist(ageDist)
                .genderDist(genderDist)
                .lastUpdatedAt(LocalDateTime.now())
                .build();
    }

    public DashboardDTO getStats() {
        if (cache == null) {
            refreshStats();
        }
        return cache;
    }


}
