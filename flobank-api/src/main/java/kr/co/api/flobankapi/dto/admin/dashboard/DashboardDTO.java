package kr.co.api.flobankapi.dto.admin.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDTO {

    private int todayVisitCount;    //null
    private int recent5mVisitCount;  //null

    // 오늘 거래 건수 (입출금 / 환전 / 외화송금 )
    private List<TxCountDTO> todayTxCounts;

    // 가입자 수 (일/주/월)
    private List<JoinStatsDTO> dailyJoin;
    private List<JoinStatsDTO> weeklyJoin;
    private List<JoinStatsDTO> monthlyJoin;

    // 연령/성별 분포
    private List<AgeBandDTO> ageDist;
    private List<GenderStatsDTO> genderDist;

    // 기준 시각 (5분 단위 갱신 시간)
    private LocalDateTime lastUpdatedAt;
}
