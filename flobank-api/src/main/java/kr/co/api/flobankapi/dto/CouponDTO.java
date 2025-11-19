package kr.co.api.flobankapi.dto;

import lombok.Data;

@Data
public class CouponDTO {

    private Integer coupNo;
    private String coupCustCode;
    private Integer coupType;
    private Integer coupRate;
    private String coupIssuedDy;
    private Integer coupStatus;


}
