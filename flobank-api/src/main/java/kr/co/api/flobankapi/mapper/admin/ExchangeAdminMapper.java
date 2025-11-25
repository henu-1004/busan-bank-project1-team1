package kr.co.api.flobankapi.mapper.admin;

import kr.co.api.flobankapi.dto.admin.exchange.CouponIssueDTO;
import kr.co.api.flobankapi.dto.admin.exchange.CurrencyDailyAmountDTO;
import kr.co.api.flobankapi.dto.admin.exchange.DailyExchangeAmountDTO;
import kr.co.api.flobankapi.dto.admin.exchange.ExchangeHistoryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ExchangeAdminMapper {

    List<CurrencyDailyAmountDTO> selectDailyCurrencyAmounts();

    List<DailyExchangeAmountDTO> selectDailyTotalAmounts();

    LocalDateTime selectStatsBaseTime();

    int countExchangeHistory(@Param("searchType") String searchType, @Param("keyword") String keyword);

    List<ExchangeHistoryDTO> selectExchangeHistory(@Param("searchType") String searchType,
                                                   @Param("keyword") String keyword,
                                                   @Param("start") int start,
                                                   @Param("end") int end);

    int countCouponIssues();

    List<CouponIssueDTO> selectCouponIssues(@Param("start") int start, @Param("end") int end);
}