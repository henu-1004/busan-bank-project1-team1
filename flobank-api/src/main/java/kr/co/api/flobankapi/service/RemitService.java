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
                // [수정] 원화 계좌인 경우: (외화금액 * 환율) + 수수료(원화) 차감
                // getRemtAmount는 외화 기준이므로 환율을 곱해야 함
                double krwAmount = frgnRemtTranDTO.getRemtAmount() * frgnRemtTranDTO.getRemtAppliedRate();
                // 수수료까지 포함하여 차감할지, 수수료는 별도인지 정책에 따름 (보통 총 출금액 차감)
                // 여기서는 수수료 포함하여 차감하는 로직으로 작성
                int totalDeductAmount = (int)(krwAmount + frgnRemtTranDTO.getRemtFee());

                mypageMapper.updateMinusAcct(totalDeductAmount, frgnRemtTranDTO.getRemtAcctNo());
            } else {
                // 외화 계좌인 경우: 외화금액 + 수수료(외화) 차감
                double totalDeductAmount = frgnRemtTranDTO.getRemtAmount() + frgnRemtTranDTO.getRemtFee();
                remitMapper.updateMinusBal(totalDeductAmount, frgnRemtTranDTO.getRemtAcctNo());
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
