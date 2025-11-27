package kr.co.api.flobankapi.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QnaDTO {

    private Long qnaNo;          // QNA_NO
    private String qnaCustCode;  // QNA_CUST_CODE
    private String qnaTitle;     // QNA_TITLE
    private String qnaContent;   // QNA_CONTENT
    private String qnaDraft;     // QNA_DRAFT
    private String qnaReply;     // QNA_REPLY
    private LocalDateTime qnaDt; // QNA_DT
    private Integer qnaHit;      // QNA_HIT
}
