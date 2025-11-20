package kr.co.api.flobankapi.dto.admin.dashboard;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TxCountDTO {

    private String type;        // "입출금", "환전", "외화송금"
    private long count;
}
