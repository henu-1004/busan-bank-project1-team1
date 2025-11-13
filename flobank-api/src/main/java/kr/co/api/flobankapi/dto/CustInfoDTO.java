package kr.co.api.flobankapi.dto;

import lombok.Data;

@Data
public class CustInfoDTO {
    private String custCode;
    private String custId;
    private String custPw;      // 단방향 암호화
    private String custName;
    private String custJumin;   // 양방향 암호화
    private String custEmail;
    private String custHp;      // 양방향 암호화
    private String custBirthDt;
    private String custGen;
    private String custEngName;
    private String custRegDt;
    private Integer custStatus;
    private String custZip;
    private String custAddr1;
    private String custAddr2;
    private Integer custSecurityLevel; // 보안 등급
    private String custLasLoginDt;
}
