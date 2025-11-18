package kr.co.api.flobankapi.service;

import kr.co.api.flobankapi.dto.CustAcctDTO;
import kr.co.api.flobankapi.dto.CustInfoDTO;
import kr.co.api.flobankapi.mapper.MemberMapper;
import kr.co.api.flobankapi.mapper.MypageMapper;
import kr.co.api.flobankapi.util.AesUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class MypageService {
    private final MemberMapper memberMapper;
    private final MypageMapper mypageMapper;
    private final PasswordEncoder passwordEncoder;

    public void saveAcct(CustAcctDTO custAcctDTO) { // 계좌 생성
        // 계좌 비밀번호 암호화 => 단방향
        String endPw = passwordEncoder.encode(custAcctDTO.getAcctPw());
        custAcctDTO.setAcctPw(endPw);

        mypageMapper.insertAcct(custAcctDTO);
    }

    public CustInfoDTO getCustInfo(String custId) { // 고객 정보 받아오기
        CustInfoDTO custInfoDTO = memberMapper.findByIdCustInfo(custId);

        String decHp = AesUtil.decrypt(custInfoDTO.getCustHp());

        // 전화번호 마스킹 처리 (정규식 사용)
        // 이 정규식은 '01012345678'과 '010-1234-5678' 형식을 모두 '010-****-5678'로 변환합니다.
        String maskedHp = decHp.replaceAll("^(\\d{3})-?(\\d{4})-?(\\d{4})$", "$1-****-$3");

        custInfoDTO.setCustHp(decHp); // 마스킹 안된 전화번호
        custInfoDTO.setCustMaskHp(maskedHp); // 마스킹 된 전화번호

        return custInfoDTO;
    }

    public boolean checkKoAcct(String custCode) {

        LocalDate latestRegDt = mypageMapper.selectCheckKoAcct(custCode);

        // 1. 개설 이력이 없으면 -> 즉시 개설 가능
        if (latestRegDt == null) {
            return true;
        }

        // 2. 개설 이력이 있으면 -> 1개월(30일)이 지났는지 확인
        LocalDate today = LocalDate.now();
        long daysPassed = ChronoUnit.DAYS.between(latestRegDt, today);

        // 30일(1개월) 미만이면 false(개설 불가), 이상이면 true(개설 가능)
        return daysPassed >= 30;
    }

    public int checkCntKoAcct(String custCode) {
        return mypageMapper.selectCheckCntKoAcct(custCode);
    }

}
