package kr.co.api.flobankapi.dto;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyInfoDTO {
    private int curNo;
    private String curName;
    private String curCode;
}
