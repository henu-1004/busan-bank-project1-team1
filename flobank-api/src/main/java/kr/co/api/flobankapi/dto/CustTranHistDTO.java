package kr.co.api.flobankapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.text.NumberFormat;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustTranHistDTO {
    private Integer tranNo;
    private String tranAcctNo;
    private String tranCustName;
    private Integer tranType;
    private BigDecimal tranAmount;
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

    // 예금 이체화면 출력용
    private String tranExpAcctNo;

    // 추가납입용
    private BigDecimal dpstDtlAmount;
    private BigDecimal dpstDtlAppliedRate;

    public String getFormattedCurrency() {
        String symbol = switch (tranCurrency) {
            case "KRW" -> "₩";
            case "USD", "AUD" -> "$";
            case "CNH", "CNY", "JPY" -> "¥";
            case "GBP" -> "£";
            case "EUR" -> "€";
            default -> "?";
        };
        return symbol;
    }


}
