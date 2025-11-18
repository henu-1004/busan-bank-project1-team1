package kr.co.api.flobankapi.dto;

import lombok.Data;

@Data
public class TermsHistDTO {
    private Long thistNo;
    private Integer thistTermCate;
    private Integer thistTermOrder;

    private String thistFile;
    private String thistContent;
    private Integer thistVersion;

    private String thistVerMemo;
    private String thistAdminId;
    private String thistRegDy;
}
