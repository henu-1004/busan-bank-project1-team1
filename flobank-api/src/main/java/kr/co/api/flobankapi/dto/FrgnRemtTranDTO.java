package kr.co.api.flobankapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FrgnRemtTranDTO {
    long remtNo;
    String remtAcctNo;
    String remtCustName;
    double remtAmount;
    String remtDt;
    String remtRecAccNo;
    String remtRecName;
    String remtRecBkCode;
    String remtCurrency;
    double remtAppliedRate;
    double remtFee;
    int remtStatus;
    String remtAddr;
    String remtCity;
    String remtState;
    String remtApproveNo;
    String remtEsignYn;
    String remtEsignDt;
    String remtCountry;
}
