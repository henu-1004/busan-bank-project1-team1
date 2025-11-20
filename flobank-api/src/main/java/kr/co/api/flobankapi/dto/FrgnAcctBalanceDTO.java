package kr.co.api.flobankapi.dto;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class FrgnAcctBalanceDTO {

    String balNo;
    String balCurrency;
    Integer balBalance;
    String balRegDt;

    String balFrgnAcctNo; // 모체 통장 계좌번호
}
