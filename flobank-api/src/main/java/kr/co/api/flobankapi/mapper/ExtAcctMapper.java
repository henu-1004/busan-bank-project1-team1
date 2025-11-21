package kr.co.api.flobankapi.mapper;

import kr.co.api.flobankapi.dto.ExtAcctDTO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExtAcctMapper {
    // 국내 외부 은행 단건 조회
    ExtAcctDTO selectExtAcct(String acctNo, String bkCode);
}
