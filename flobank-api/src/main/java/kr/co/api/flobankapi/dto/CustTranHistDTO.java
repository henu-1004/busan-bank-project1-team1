package kr.co.api.flobankapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustTranHistDTO {
    private Integer tranNo;
    private String tranAcctNo;
    private String tranCustName;
    private Integer tranType;
    private Integer tranAmount;
    private String tranDt;
    private String tranRecAcctNo;
    private String tranRecName;
    private String tranRecBkCode;
    private String tranMemo;
    private String tranApproveNo;
    private String tranEsignYn; // 전자서명 y / n
    private String tranEsignDt; // 전자서명 받은 날짜
    private String tranCurrency;


    private Integer tranBalance;

}
