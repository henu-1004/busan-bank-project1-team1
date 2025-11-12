package kr.co.api.flobankapi.dto;

import lombok.Data;

@Data
public class CustAcctDTO {
    String acctNo;
    String acctPw;
    Integer acctBalance;
    String acctRegDt;
    String acctStatus;
    String acctCustCode;
}
