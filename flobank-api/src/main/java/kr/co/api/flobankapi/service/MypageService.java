package kr.co.api.flobankapi.service;

import kr.co.api.flobankapi.dto.*;
import kr.co.api.flobankapi.mapper.MemberMapper;
import kr.co.api.flobankapi.mapper.MypageMapper;
import kr.co.api.flobankapi.util.AesUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
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
        String[] currency = {"USD", "JPY", "EUR", "CNH", "GBP", "AUD"};
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

    // 외화 자식 계좌 조회
    public List<FrgnAcctBalanceDTO> getAllFrgnAcctBal(String frgnAcctNo) {
        return mypageMapper.selectAllFrgnAcctBal(frgnAcctNo);
    }

    public CustInfoDTO getCustInfo(String userCode) {
        // 1. DB에서 고객 정보 조회 (MypageMapper 하나만 사용)
        CustInfoDTO custInfo = mypageMapper.selectCustInfoByCode(userCode);

        if (custInfo == null) {
            return null;
        }

        // 2. 이메일 복호화 시도
        if (custInfo.getCustEmail() != null) {
            try {
                String decryptedEmail = AesUtil.decrypt(custInfo.getCustEmail());
                custInfo.setCustEmail(decryptedEmail);
            } catch (Exception e) {
                // 복호화 실패 시(암호화 안 된 평문일 경우 등) 로그만 남기고 원래 값 유지
            }
        }

        // 3. 휴대폰 번호 복호화 시도
        if (custInfo.getCustHp() != null) {
            try {
                String decryptedHp = AesUtil.decrypt(custInfo.getCustHp());
                custInfo.setCustHp(decryptedHp); // 복호화 성공 시 덮어쓰기
            } catch (Exception e) {
                // 복호화 실패 시 원래 값 유지
            }
        }



        return custInfo;
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


    // 비밀번호 검증
    public boolean checkPassword(String userCode, String rawPassword) {
        // 1. DB에서 고객 정보 조회
        CustInfoDTO custInfo = mypageMapper.selectCustInfoByCode(userCode);

        if (custInfo == null) {
            return false;
        }

        String dbPw = custInfo.getCustPw();

        // [디버깅용 로그] 콘솔에서 이 값이 어떻게 찍히는지 꼭 확인해보세요!

        // 2. 암호화된 비밀번호 비교 (정석 방법)
        if (passwordEncoder.matches(rawPassword, dbPw)) {

            return true;
        }

        // 3. [테스트용] 혹시 DB에 그냥 생으로(평문) 저장되어 있나요? 그럼 이걸로 통과!
        if (rawPassword.equals(dbPw)) {
            return true;
        }
        return false;
    }


    // 회원 정보 수정
    public void modifyUserInfo(String userCode, Map<String, String> data) {

        // 1. 업데이트할 정보를 담을 DTO 생성
        CustInfoDTO updateDto = new CustInfoDTO();
        updateDto.setCustCode(userCode); // 누구를 업데이트할지 식별

        updateDto.setCustEmail(data.get("email"));
        updateDto.setCustHp(data.get("hp"));
        updateDto.setCustZip(data.get("zipcode"));
        updateDto.setCustAddr1(data.get("addr1"));
        updateDto.setCustAddr2(data.get("addr2"));

        // 2. 비밀번호 변경 요청이 있는 경우 (값이 존재하면)
        String newPw = data.get("newPassword");
        if (newPw != null && !newPw.trim().isEmpty()) {
            // 암호화 수행
            String encodedPw = passwordEncoder.encode(newPw);
            updateDto.setCustPw(encodedPw);
        }

        // 3. DB 업데이트 실행
        mypageMapper.updateCustInfo(updateDto);
    }


    // 계좌 거래내역 조회 (현재 잔액 기준으로 과거 잔액 역산)
    public Map<String, Object> getAcctDetailWithHistory(String acctNo) {
        Map<String, Object> result = new HashMap<>();

        // 1. 계좌 기본 정보 조회 (여기서 현재 잔액 ACCT_BALANCE를 가져옴)
        CustAcctDTO account = mypageMapper.selectCustAcct(acctNo);

        // 2. 거래 내역 조회 (최신순 정렬 필수: ORDER BY TRAN_DT DESC)
        List<CustTranHistDTO> historyList = mypageMapper.selectTranHist(acctNo);

        // 3. [핵심] 잔액 역산 로직
        // DB에 있는 '현재 잔액'을 시작점으로 잡습니다.
        long calculatorBalance = account.getAcctBalance();

        for (CustTranHistDTO hist : historyList) {
            // (1) 현재 리스트의 row에 잔액을 세팅 (이 시점의 잔액은 이거였다!)
            hist.setTranBalance((int) calculatorBalance);

            // (2) 다음(더 과거) row를 위해 잔액을 되돌림
            // 최신 거래가 '입금(1)'이었다면 -> 입금 전에는 돈이 적었을 테니 뺌
            if (hist.getTranType() == 1) {
                calculatorBalance = calculatorBalance - hist.getTranAmount();
            }
            // 최신 거래가 '출금(2)'이었다면 -> 출금 전에는 돈이 많았을 테니 더함
            else if (hist.getTranType() == 2) {
                calculatorBalance = calculatorBalance + hist.getTranAmount();
            }
        }

        result.put("account", account);
        result.put("history", historyList);

        return result;
    }


    public List<CouponDTO> getCouponList(String custCode) {
        return mypageMapper.selectCouponList(custCode);
    }

}



