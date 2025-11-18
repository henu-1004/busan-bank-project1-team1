package kr.co.api.flobankapi.dto;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotSessionDTO {
    private String sessId;
    private String sessCustCode;
    private String sessStartDt;
}
