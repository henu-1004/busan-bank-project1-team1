package kr.co.api.flobankapi.dto;

import lombok.Data;

@Data
public class TermsDTO {
    private Integer termCate;
    private Integer termOrder;
    private String termTitle;
    private String termContent;
}
