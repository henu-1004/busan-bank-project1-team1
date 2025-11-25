package kr.co.api.flobankapi.dto.admin.exchange;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeHistoryDTO {
    private Long exchNo;
    private String custCode;
    private String exchFromCurrency;
    private String exchToCurrency;
    private BigDecimal exchAppliedRate;
    private LocalDateTime lastUpdatedAt;
}