package kr.co.api.flobankapi.dto;

import lombok.Data;

public class TermsDTO {
    private Long termNo;
    private String termTitle;
    private String termFile;
    private String termRegDy;
    private Integer termType;

    private String termContent;

    public Long getTermNo() {
        return termNo;
    }

    public String getTermTitle() {
        return termTitle;
    }

    public String getTermFile() {
        return termFile;
    }

    public String getTermRegDy() {
        return termRegDy;
    }

    public Integer getTermType() {
        return termType;
    }

    public String getTermContent() {
        return termContent;
    }

    public void setTermNo(Long termNo) {
        this.termNo = termNo;
    }

    public void setTermRegDy(String termRegDy) {
        this.termRegDy = termRegDy;
    }

    public void setTermFile(String termFile) {
        this.termFile = termFile;
    }

    public void setTermTitle(String termTitle) {
        this.termTitle = termTitle;
    }

    public void setTermType(Integer termType) {
        this.termType = termType;
    }

    public void setTermContent(String termContent) {
        this.termContent = termContent;
    }
}
