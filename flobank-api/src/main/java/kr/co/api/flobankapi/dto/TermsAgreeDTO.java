package kr.co.api.flobankapi.dto;


import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TermsAgreeDTO {
    private Long agreeNo;
    private String agreeCustCode;

    private Integer agreeTermCate;
    private Integer agreeTermOrder;

    private String agreeYn;
    private LocalDateTime agreeDt;
}
