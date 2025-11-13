package kr.co.api.flobankapi.dto;

import lombok.Data;

@Data
public class CustInfoDTO {
    private String custCode;
    private String custId;
    private String custPw;
    private String custName;
    private String custJumin;
    private String custEmail;
    private String custHp;
    private String custBirthDt;
    private String custGen;
    private String custEngName;
    private String custRegDt;
    private String custStatus;
    private String custZip;
    private String custAddr1;
    private String custAddr2;
    private String custSecurityLevel; // 보안 등급
    private String custLasLoginDt;
}
