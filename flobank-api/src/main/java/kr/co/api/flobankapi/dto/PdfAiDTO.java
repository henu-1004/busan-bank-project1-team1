package kr.co.api.flobankapi.dto;

import lombok.Data;

@Data
public class PdfAiDTO {
    private Long pdfId;
    private String orgFileName;
    private String storedFileName;
    private String filePath;
    private String extractedText;
    private String aiComment;
    private String status;
    private String errorMessage;

    //추가
    private String downloadUrl;

}