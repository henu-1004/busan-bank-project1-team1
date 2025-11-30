package kr.co.api.flobankapi.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatbotAdminDTO {
    int botNo;
    String userQuestion;
    String botAnswer;
    String botDt;
}
