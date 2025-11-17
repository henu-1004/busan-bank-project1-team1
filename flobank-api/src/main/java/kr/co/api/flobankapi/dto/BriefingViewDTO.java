package kr.co.api.flobankapi.dto;

import lombok.Data;

@Data
public class BriefingViewDTO {
    private String briefingMode;
    private String briefingTitle;
    private String briefingDateText;
    private String[] briefingLines;
}
