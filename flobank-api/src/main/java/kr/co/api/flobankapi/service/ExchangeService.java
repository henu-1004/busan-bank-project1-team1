package kr.co.api.flobankapi.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.api.flobankapi.dto.*;
import kr.co.api.flobankapi.jwt.CustomUserDetails;
import kr.co.api.flobankapi.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ExchangeService {

    private final RateService rateService;
    private final ObjectMapper objectMapper; // JSON 파싱용 (Spring Boot 기본 내장)
    private final MypageMapper mypageMapper;
    private final ExchangeMapper exchangeMapper;
    private final PasswordEncoder passwordEncoder;
    private final CustAcctMapper  custAcctMapper;
    private final FrgnAcctMapper frgnAcctMapper;
    private final CouponMapper couponMapper;

    public BigDecimal calculateExchange(String date, String targetCurrency, BigDecimal krwAmount) {
        try {
            // 1. Redis 또는 API에서 환율 JSON 데이터 가져오기
            String jsonResponse = rateService.getRate(date);

            if (jsonResponse == null) {
                throw new RuntimeException("환율 데이터를 가져올 수 없습니다.");
            }

            // 2. JSON 문자열을 DTO 리스트로 변환
            List<RateInfoDTO> rateList = objectMapper.readValue(jsonResponse, new TypeReference<List<RateInfoDTO>>() {});

            // 3. 리스트에서 내가 원하는 통화(예: USD) 찾기
            RateInfoDTO targetRateInfo = rateList.stream()
                    .filter(rate -> rate.getCurUnit().equals(targetCurrency))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("해당 통화의 환율 정보를 찾을 수 없습니다: " + targetCurrency));

            // 4. 환율 문자열(예: "1,350.50")에서 쉼표 제거 후 숫자로 변환
            String rateStr = targetRateInfo.getDealBasR().replace(",", "");
            BigDecimal exchangeRate = new BigDecimal(rateStr);

            // 5. 환전 계산 (원화 금액 / 매매기준율)
            // 소수점 2자리까지 반올림 처리
            return krwAmount.divide(exchangeRate, 2, RoundingMode.HALF_UP);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("환전 계산 중 오류 발생: " + e.getMessage());
        }
    }

    // 고객 보유 전체 원화 계좌 확인
    public List<CustAcctDTO> getAllKoAcct(String custCode) {

        return mypageMapper.selectAllKoAcct(custCode);
    }

    // 고객 보유 전체 쿠폰 확인
    public List<CouponDTO> getCoupons(String custCode) {
        return exchangeMapper.selectAllCoupon(custCode);
    }

    // 환전 신청했을 때 환전 테이블 삽입, 계좌 잔액 업데이트, 계좌 이체 내역 테이블 삽입, 쿠폰 상태 업데이트
    @Transactional
    public void processExchange(FrgnExchTranDTO transDTO) {

        // 1. 환전 내역 테이블 INSERT (기존 값 그대로)
        exchangeMapper.insertExchange(transDTO);

        // 2. 사용자 이름 가져오기 (거래내역 기록용)
        String custName = getAuthenticatedUserName();

        // 3. 거래 유형별(BUY/SELL) 로직 분기
        if ("KRW".equals(transDTO.getExchFromCurrency())) {
            // ==========================================
            // CASE A: 원화 -> 외화 (BUY)
            // ==========================================

            // 1) 원화 출금액 계산 (소수점 절사)
            // 공식: 외화금액 * 적용환율
            BigDecimal withdrawKrw = transDTO.getExchAmount()
                    .multiply(transDTO.getExchAppliedRate())
                    .setScale(0, RoundingMode.FLOOR);

            // 2) 원화 계좌 잔액 차감 (출금)
            custAcctMapper.updateAcctBal(withdrawKrw, transDTO.getExchAcctNo());

            // 3) 원화 계좌 거래내역 INSERT (출금)
            // 적요: "환전(USD)"
            insertTransactionHistory(
                    transDTO.getExchAcctNo(),
                    custName,
                    "환전(" + transDTO.getExchToCurrency() + ")",
                    withdrawKrw,
                    2 // 2: 출금
            );

        } else {
            // ==========================================
            // CASE B: 외화 -> 원화 (SELL)
            // ==========================================

            // 1) 외화 출금액 (소수점 유지)
            BigDecimal withdrawForeign = transDTO.getExchAmount();

            // 2) 원화 입금액 계산 (소수점 절사)
            BigDecimal depositKrw = withdrawForeign
                    .multiply(transDTO.getExchAppliedRate())
                    .setScale(0, RoundingMode.FLOOR);

            // 3) 외화 계좌 잔액 차감 (출금)
            // (주의: custAcct.xml에 updateFrgnAcctBal 쿼리가 있어야 함)
            frgnAcctMapper.updateFrgnAcctBal(
                    withdrawForeign,
                    transDTO.getExchAcctNo(),
                    transDTO.getExchFromCurrency() // 예: "USD"
            );

            // 4) 원화 계좌 잔액 증가 (입금)
            // 주소 필드("즉시입금:110-...")에서 입금 계좌번호 추출
            String depositAcctNo = transDTO.getExchAddr().replace("즉시입금:", "").trim();
            mypageMapper.updatePlusAcctBig(depositKrw, depositAcctNo);

            // 5) 원화 계좌 거래내역 INSERT (입금)
            // 적요: "환전입금(USD)"
            insertTransactionHistory(
                    depositAcctNo,
                    custName,
                    "환전입금(" + transDTO.getExchFromCurrency() + ")",
                    depositKrw,
                    1 // 1: 입금
            );

            /* * [참고] 외화 계좌의 출금 내역 기록
             * 외화는 소수점(센트)이 있으므로 정수형인 TB_CUST_TRAN_HIST에 넣으면 데이터가 손실됩니다.
             * 별도의 외화 거래내역 테이블(TB_FRGN_TRAN_HIST 등)이 있다면 여기서 insert를 수행해야 합니다.
             */
        }

        // 4. 쿠폰 상태 업데이트 (사용 처리)
        if (transDTO.getCouponNo() != null && transDTO.getCouponNo() > 0) {
            couponMapper.updateCouponStatus(transDTO.getCouponNo());
        }
    }

    // [Helper] 거래내역 기록 공통 메소드
    private void insertTransactionHistory(String acctNo, String custName, String summary, BigDecimal amount, int type) {
        CustTranHistDTO histDTO = new CustTranHistDTO();
        histDTO.setTranAcctNo(acctNo);
        histDTO.setTranCustName(custName); // 예: 홍길동
        histDTO.setTranMemo(summary);      // 예: 환전(USD) -> 적요 필드가 있다면 여기에, 없으면 Name에 덮어쓰기 고려

        // 만약 화면에 "환전(USD)"를 표시하고 싶다면 TranRecName(상대방명)이나 Memo에 넣어야 합니다.
        // 여기서는 요청하신대로 "환전이라는 값이 출력될 수 있도록" TranRecName에 넣습니다.
        histDTO.setTranRecName(summary);

        histDTO.setTranType(type);         // 1:입금, 2:출금
        histDTO.setTranAmount(amount); // 원화는 정수

        // NULL 처리 (MyBatis 에러 방지용)
        histDTO.setTranRecAcctNo("");
        histDTO.setTranRecBkCode("");
        histDTO.setTranEsignYn("Y");

        mypageMapper.insertTranHist(histDTO);
    }

    // [Helper] 인증된 사용자 이름 가져오기
    private String getAuthenticatedUserName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            return ((CustomUserDetails) authentication.getPrincipal()).getCustName();
        }
        return "Unknown";
    }

    // 환전하기 전 계좌 비밀번호 일치하는지 확인
    public boolean checkAccountPassword(String acctNo, String acctPass, String mode) {

        if("BUY".equals(mode)){ // 원화 -> 외화
            CustAcctDTO custAcctDTO = mypageMapper.selectCustAcct(acctNo);
            if (custAcctDTO == null) {
                return false;
            }
            return passwordEncoder.matches(acctPass, custAcctDTO.getAcctPw());
        }
        else{                  // 외화 -> 원화
            CustFrgnAcctDTO custFrgnAcctDTO = frgnAcctMapper.selectFrgnAcctByAcctNo(acctNo);
            if (custFrgnAcctDTO == null) {
                return false;
            }
            return passwordEncoder.matches(acctPass, custFrgnAcctDTO.getFrgnAcctPw());
        }
    }

}
