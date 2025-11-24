package kr.co.api.flobankapi.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDate;

@Data
@Document(indexName = "docs")
@Setting(settingPath = "elastic/common-setting.json")
@Mapping(mappingPath = "elastic/terms-mapping.json")
public class TermDocument {

    @Id
    private String thistNo;       // 히스토리 번호 (PK)

    @Field(type = FieldType.Text, analyzer = "nori")
    private String termTitle;     // 약관 제목 (from MASTER)

    @Field(type = FieldType.Text, analyzer = "nori")
    private String thistContent;  // 약관 내용 (from HIST - CLOB)

    // 버전 (검색보다는 필터/표시용이므로 Keyword 타입 추천)
    @Field(type = FieldType.Keyword)
    private String thistVersion;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate thistRegDy;

    @Field(type = FieldType.Text, analyzer = "nori")
    private String thistFile;

    // 자동완성을 위한 필드
    @CompletionField(maxInputLength = 100, preserveSeparators = true)
    private Completion suggest;
}