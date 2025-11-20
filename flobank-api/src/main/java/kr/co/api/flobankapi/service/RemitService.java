package kr.co.api.flobankapi.service;

import kr.co.api.flobankapi.dto.CustFrgnAcctDTO;
import kr.co.api.flobankapi.dto.FrgnAcctBalanceDTO;
import kr.co.api.flobankapi.dto.FrgnRemtTranDTO;
import kr.co.api.flobankapi.mapper.MypageMapper;
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
    private final MypageMapper mypageMapper;

    // 외화 이체
    @Transactional
    public boolean saveFrgnTran(FrgnRemtTranDTO frgnRemtTranDTO){
        try {
            // 이체 내역 추가
            remitMapper.insertFrgnRemtTran(frgnRemtTranDTO);

            // 계좌 잔액 처리
            if(frgnRemtTranDTO.getRemtAcctNo().contains("-10-")){
                Integer bal = Math.toIntExact(Math.round(frgnRemtTranDTO.getRemtAmount()));
                mypageMapper.updateMinusAcct(bal, frgnRemtTranDTO.getRemtAcctNo());
            } else {
                remitMapper.updateMinusBal(frgnRemtTranDTO.getRemtAmount(), frgnRemtTranDTO.getRemtAcctNo());
            }

        }catch (Exception e){
            e.printStackTrace();
            return false;
        }

        return true;

    }

    // 모체 계좌번호 가져오기
    public CustFrgnAcctDTO getParAcctNo(String balNo) {
        return remitMapper.selectParAcctNo(balNo);
    }
}
