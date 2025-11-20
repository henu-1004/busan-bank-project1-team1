package kr.co.api.flobankapi.mapper;

import kr.co.api.flobankapi.dto.*;
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

    // 계좌 조회
    CustAcctDTO selectCustAcct(String acctNo);

    // 계좌 이체 성공시 삽입
    void insertTranHist(CustTranHistDTO custTranHistDTO);

    // 입금
    void updatePlusAcct(Integer amount, String acctNo);

    // 출금
    void updateMinusAcct(Integer amount, String acctNo);

    // 원화 입출금 계좌 개수 확인
    Integer selectCntAcct(String custCode);

    // 외화 자식 계좌 생성
    void insertAllFrgnAcctBal(List<FrgnAcctBalanceDTO> item);

    // 외화 자식 계좌 조회
    List<FrgnAcctBalanceDTO> selectAllFrgnAcctBal(String frgnAcctNo);

    CustInfoDTO selectCustInfo(String custId);
    LocalDate selectCheckKoAcct(String custCode);
    int selectCheckCntKoAcct(String custCode);
}
