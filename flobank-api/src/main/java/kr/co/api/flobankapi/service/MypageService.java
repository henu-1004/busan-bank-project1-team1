package kr.co.api.flobankapi.service;

import kr.co.api.flobankapi.dto.*;
import kr.co.api.flobankapi.mapper.MemberMapper;
import kr.co.api.flobankapi.mapper.MypageMapper;
import kr.co.api.flobankapi.util.AesUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

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

        // 이미 있는 입출금통장 개수 들고오기
        int cnt = findCntAcct(custAcctDTO.getAcctCustCode());

        // 기본 이름 설정("FLO 입출금통장 + (n+1)")
        custAcctDTO.setAcctName("FLO 입출금통장" + (cnt + 1));

        mypageMapper.insertAcct(custAcctDTO);
    }

    // 외화 입출금계좌 생성
    @Transactional
    public void saveFrgnAcct(CustFrgnAcctDTO custFrgnAcctDTO) {

        // 계좌 비밀번호 암호화 = > 단방향
        String endPw = passwordEncoder.encode(custFrgnAcctDTO.getFrgnAcctPw());
        custFrgnAcctDTO.setFrgnAcctPw(endPw);

        // 기본 외화 통장 이름 설정
        String name = "FLO 외화통장";
        custFrgnAcctDTO.setFrgnAcctName(name);

        mypageMapper.insertFrgnAcct(custFrgnAcctDTO);

        // 생성된 외화 부모 계좌 들고오기
        CustFrgnAcctDTO frgnAcctDTO = mypageMapper.selectFrgnAcct(custFrgnAcctDTO.getFrgnAcctCustCode());
        // 자식 통장 만들기
        String[] currency = {"USD", "JPY", "EUR", "CNY", "GBP", "AUD"};
        List<FrgnAcctBalanceDTO> frgnAcctBalanceList = new ArrayList<>();
        for(String c : currency){
            FrgnAcctBalanceDTO frgnAcctBalance = new FrgnAcctBalanceDTO();
            frgnAcctBalance.setBalCurrency(c);
            frgnAcctBalance.setBalFrgnAcctNo(frgnAcctDTO.getFrgnAcctNo());

            frgnAcctBalanceList.add(frgnAcctBalance);
        }
        mypageMapper.insertAllFrgnAcctBal(frgnAcctBalanceList);

    }

    // 외화 입출금 통장 이미 있는지 확인
    public int checkEnAcct(String custCode) {
        return mypageMapper.selectCheckCntEnAcct(custCode);
    }

    // 고객 보유 전체 계좌 확인
    public List<CustAcctDTO> findAllAcct(String custCode) {
        return mypageMapper.selectAllKoAcct(custCode);
    }

    // 고객 외환 계좌 확인
    public CustFrgnAcctDTO findFrgnAcct(String custCode) {
        return mypageMapper.selectFrgnAcct(custCode);
    }

    // 원화 계좌 이름 바꾸기
    public void modifyAcctName(String name, String acctNo) {
        mypageMapper.updateAcctName(name, acctNo);
    }

    // 외화 계좌 이름 바꾸기
    public void modifyFrgnAcctName(String name, String acctNo) {
        mypageMapper.updateFrgnAcctName(name, acctNo);
    }

    // 계좌 단건 조회
    public CustAcctDTO findCustAcct(String acctNo) {
        return mypageMapper.selectCustAcct(acctNo);
    }

    // 원화 입출금 계좌 개수 확인
    public Integer findCntAcct(String custCode) {
        return  mypageMapper.selectCntAcct(custCode);
    }

    // 입금, 출금
    @Transactional
    public void modifyCustAcctBal(CustTranHistDTO custTranHistDTO){
        Integer amount = custTranHistDTO.getTranAmount();
        String acctNo = custTranHistDTO.getTranAcctNo();
        mypageMapper.updateMinusAcct(amount, acctNo);

        // 출금을 플로뱅크로 했다면
        if("888".equals(custTranHistDTO.getTranRecBkCode())){
            String recAcctNo = custTranHistDTO.getTranRecAcctNo();
            mypageMapper.updatePlusAcct(amount, recAcctNo);
        }

        // 이체 내역 삽입
        mypageMapper.insertTranHist(custTranHistDTO);
    }

    public CustInfoDTO getCustInfo(String userCode) { // 고객 정보 받아오기
        CustInfoDTO custInfoDTO = memberMapper.findByCodeCustInfo(userCode);

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
