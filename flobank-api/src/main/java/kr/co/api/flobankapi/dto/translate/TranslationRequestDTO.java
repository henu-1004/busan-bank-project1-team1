package kr.co.api.flobankapi.dto.translate;

import lombok.Data;

@Data
public class TranslationRequestDTO {
    private String text;
    private String targetLang;
}
