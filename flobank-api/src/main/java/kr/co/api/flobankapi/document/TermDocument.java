package kr.co.api.flobankapi.document;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
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

    @JsonProperty("thistFile")  // JSON에서 "thistFile"이라는 키를 강제로 매핑
    @Field(name = "thistFile", type = FieldType.Keyword) // ES 필드명도 확실하게 지정
    private String thistFile;

    // 자동완성을 위한 필드
    @CompletionField(maxInputLength = 100, preserveSeparators = true)
    private Completion suggest;
}