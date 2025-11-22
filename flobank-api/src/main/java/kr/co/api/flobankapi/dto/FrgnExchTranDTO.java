package kr.co.api.flobankapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FrgnExchTranDTO {
    private long exchNo;
    private String exchAcctNo;
    private String exchFromCurrency;
    private String exchToCurrency;
    private Integer exchAmount;
    private double exchAppliedRate;
    private String exchReqDt;
    private String exchExpDy;
    private String exchAddr;
    private double exchFee;
    private String exchEsignYn;
    private String exchEsignDt;
}
