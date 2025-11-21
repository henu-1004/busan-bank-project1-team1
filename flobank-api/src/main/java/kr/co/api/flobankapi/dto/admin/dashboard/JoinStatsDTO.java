package kr.co.api.flobankapi.dto.admin.dashboard;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JoinStatsDTO {
    //가입자
    private String label;  // "2025-11-19", "2025-11", "2025-W46" 등
    private long count;

}
