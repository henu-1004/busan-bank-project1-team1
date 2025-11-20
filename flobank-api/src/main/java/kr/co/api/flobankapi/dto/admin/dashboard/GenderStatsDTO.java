package kr.co.api.flobankapi.dto.admin.dashboard;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GenderStatsDTO {

    private String gender;
    private long count;
}
