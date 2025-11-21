package kr.co.api.flobankapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustTranHistDTO {
    Integer tranNo;
    String tranAcctNo;
    String tranCustName;
    Integer tranType;
    Integer tranAmount;
    String tranDt;
    String tranRecAcctNo;
    String tranRecName;
    String tranRecBkCode;
    String tranMemo;
    String tranApproveNo;
    String tranEsignYn; // 전자서명 y / n
    String tranEsignDt; // 전자서명 받은 날짜

    private Integer tranBalance;
}
