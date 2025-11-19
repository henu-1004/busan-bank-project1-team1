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
    int balBalance;
    String balRegDt;
    String balFrgnAcctNo;

}
