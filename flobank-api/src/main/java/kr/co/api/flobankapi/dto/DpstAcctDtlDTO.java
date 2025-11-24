package kr.co.api.flobankapi.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DpstAcctDtlDTO {
    private Long dpstDtlNo;
    private Integer dpstDtlType;
    private BigDecimal dpstDtlAmount;
    private LocalDateTime dpstTranDt;
    private String dpstDtlHdrNo;
    private String dpstDtlEsignYn;
    private LocalDateTime dpstDtlEsignDt;
    private BigDecimal dpstDtlAppliedRate;
}
