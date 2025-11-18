package kr.co.api.flobankapi.mapper;

import kr.co.api.flobankapi.dto.CustInfoDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MemberMapper {
    CustInfoDTO findByIdCustInfo(String userId);
    CustInfoDTO findByCodeCustInfo(String userCode);
    void registerCustInfo(CustInfoDTO custInfoDTO);
    void insertLastLogin(String custId);
}
