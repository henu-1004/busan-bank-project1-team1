package kr.co.api.flobankapi.mapper;

import java.math.BigDecimal;

public interface CustAcctMapper {
    // 계좌 출금되었을 때 금액 업데이트
    void updateKoAcctBal(BigDecimal bal, String acctNo);
}
