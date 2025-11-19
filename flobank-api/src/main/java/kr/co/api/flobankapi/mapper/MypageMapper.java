package kr.co.api.flobankapi.mapper;

import kr.co.api.flobankapi.dto.CustAcctDTO;
import kr.co.api.flobankapi.dto.CustFrgnAcctDTO;
import kr.co.api.flobankapi.dto.CustInfoDTO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface MypageMapper {
    // 원화 입출금 개설
    void insertAcct(CustAcctDTO custAcctDTO);

    // 외화 입출금 개설
    void insertFrgnAcct(CustFrgnAcctDTO custFrgnAcctDTO);

    // 외화 계좌 이미 있는지 확인
    int selectCheckCntEnAcct(String custCode);

    // 고객 보유 전체 계좌 확인
    List<CustAcctDTO> selectAllKoAcct(String custCode);

    // 고객 외환 계좌 확인
    CustFrgnAcctDTO selectFrgnAcct(String custCode);

    // 원화 계좌명 수정
    void updateAcctName(String acctName, String acctNo);

    // 외화 계좌명 수정
    void updateFrgnAcctName(String acctName, String acctNo);

    CustInfoDTO selectCustInfo(String custId);
    LocalDate selectCheckKoAcct(String custCode);
    int selectCheckCntKoAcct(String custCode);
}
