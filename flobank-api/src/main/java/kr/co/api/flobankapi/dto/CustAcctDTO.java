package kr.co.api.flobankapi.dto;

import lombok.Data;

@Data
public class CustAcctDTO {
    private String acctNo;
    private String acctPw;
    private Integer acctBalance;
    private String acctRegDt;
    private String acctStatus;
    private String acctCustCode;
    private String acctFundSource;
    private String acctPurpose;
}
