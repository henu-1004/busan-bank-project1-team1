package kr.co.api.flobankapi.mapper;

import kr.co.api.flobankapi.dto.CustFrgnAcctDTO;
import kr.co.api.flobankapi.dto.FrgnAcctBalanceDTO;
import kr.co.api.flobankapi.dto.FrgnRemtTranDTO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RemitMapper {
    void insertFrgnRemtTran(FrgnRemtTranDTO frgnRemtTranDTO);

    void updateMinusBal(double amount, String acctNo);
    CustFrgnAcctDTO selectParAcctNo(String balNo);
}
