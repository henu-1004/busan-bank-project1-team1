package kr.co.api.flobankapi.service;

import kr.co.api.flobankapi.dto.CustFrgnAcctDTO;
import kr.co.api.flobankapi.dto.FrgnAcctBalanceDTO;
import kr.co.api.flobankapi.dto.FrgnRemtTranDTO;
import kr.co.api.flobankapi.mapper.RemitMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class RemitService {
    private final RemitMapper remitMapper;

    public void saveFrgnRemtTran(FrgnRemtTranDTO frgnRemtTranDTO) {
        remitMapper.insertFrgnRemtTran(frgnRemtTranDTO);
    }

    @Transactional
    public boolean saveFrgnTran(FrgnRemtTranDTO frgnRemtTranDTO){
        try {
            // 이체 내역 추가
            remitMapper.insertFrgnRemtTran(frgnRemtTranDTO);

            // 계좌 잔액 처리

        }catch (Exception e){
            return false;
        }

        return true;

    }

    // 모체 계좌번호 가져오기
    public CustFrgnAcctDTO getParAcctNo(String balNo) {
        return remitMapper.selectParAcctNo(balNo);
    }
}
