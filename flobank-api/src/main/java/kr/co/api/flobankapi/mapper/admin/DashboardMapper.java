package kr.co.api.flobankapi.mapper.admin;

import kr.co.api.flobankapi.dto.admin.dashboard.AgeBandDTO;
import kr.co.api.flobankapi.dto.admin.dashboard.GenderStatsDTO;
import kr.co.api.flobankapi.dto.admin.dashboard.JoinStatsDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DashboardMapper {

    // 오늘 거래 건수들
    int selectTodayWonTxCount();    //예금계좌 테이블
    int selectTodayFrgnRemtTxCount();   // 해외송금 테이블


    // 가입자 통계
    List<JoinStatsDTO> selectDailyJoinStats();
    List<JoinStatsDTO> selectWeeklyJoinStats();
    List<JoinStatsDTO> selectMonthlyJoinStats();

    // 연령/성별 분포
    List<AgeBandDTO> selectAgeDist();
    List<GenderStatsDTO> selectGenderDist();


}
