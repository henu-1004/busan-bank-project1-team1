package kr.co.api.flobankapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExtAcctDTO {
    String extNo;
    String extBkCode;
    String extBkName;
    String extCustName;
}
