package kr.co.api.flobankapi.dto;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatbotBadWordDTO {
    private Integer badId;
    private String badWord;
    private Integer badType;
    private String badUseYn;
    private LocalDateTime badRegDt;
}
