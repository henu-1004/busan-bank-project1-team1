package kr.co.api.flobankapi.dto.admin.exchange;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeStatsDTO {
    private List<CurrencyDailyAmountDTO> currencyDailyAmounts;
    private List<DailyExchangeAmountDTO> dailyTotals;
    private LocalDateTime lastUpdatedAt;
    private String rangeLabel;

}