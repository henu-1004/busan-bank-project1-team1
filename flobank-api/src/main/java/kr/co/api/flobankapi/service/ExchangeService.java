package kr.co.api.flobankapi.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.api.flobankapi.dto.*;
import kr.co.api.flobankapi.jwt.CustomUserDetails;
import kr.co.api.flobankapi.mapper.CustAcctMapper;
import kr.co.api.flobankapi.mapper.ExchangeMapper;
import kr.co.api.flobankapi.mapper.MypageMapper;
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

        // 1. 환전 내역 기록 (공통)
        exchangeMapper.insertExchange(transDTO);

        // 2. 거래 방향에 따른 분기 처리
        if ("KRW".equals(transDTO.getExchFromCurrency())) {
            // ==========================================
            // CASE A: 살 때 (원화 출금 -> 외화 수령)
            // ==========================================

            // 1) 출금할 원화 금액 계산 (소수점 절사 필수)
            BigDecimal withdrawKrw = transDTO.getExchAmount()
                    .multiply(transDTO.getExchAppliedRate())
                    .setScale(0, RoundingMode.FLOOR); // 원 단위 절사

            // 2) 원화 계좌에서 출금 (기존 매퍼 사용)
            custAcctMapper.updateKoAcctBal(withdrawKrw, transDTO.getExchAcctNo());

            // 3) 거래 내역 기록 (원화 계좌)
            insertTransactionHistory(
                    transDTO.getExchAcctNo(),
                    "환전(" + transDTO.getExchToCurrency() + ")", // 예: 환전(USD)
                    withdrawKrw,
                    2 // 출금
            );

        } else {
            // ==========================================
            // CASE B: 팔 때 (외화 출금 -> 원화 입금)
            // ==========================================

            // 1) 출금할 외화 금액 (소수점 유지!!)
            BigDecimal withdrawForeign = transDTO.getExchAmount();

            // 2) 외화 계좌에서 출금
            // [주의] 외화 계좌 테이블을 업데이트하는 별도 매퍼가 필요합니다. (아래 XML 참고)
            // custAcctMapper.updateFrgnAcctBal(withdrawForeign, transDTO.getExchAcctNo());

            // 3) 입금될 원화 금액 계산 (소수점 절사)
            BigDecimal depositKrw = withdrawForeign
                    .multiply(transDTO.getExchAppliedRate())
                    .setScale(0, RoundingMode.FLOOR);

            // 4) 입금받을 원화 계좌번호 파싱 (주소 필드에 "즉시입금:110-..." 형태로 들어옴)
            String depositAcctNo = transDTO.getExchAddr().replace("즉시입금:", "").trim();
            Integer koAmount = depositKrw.intValue();
            // 5) 원화 계좌에 입금
            // mypageMapper에 있는 updatePlusAcct 사용
            mypageMapper.updatePlusAcct(koAmount, depositAcctNo);

            // 6) 거래 내역 기록 (입금받은 원화 계좌 기준)
            insertTransactionHistory(
                    depositAcctNo,
                    "환전입금(" + transDTO.getExchFromCurrency() + ")", // 예: 환전입금(USD)
                    depositKrw,
                    1 // 입금 (보통 1번이 입금, 2번이 출금)
            );
        }
    }

    // 거래 내역 저장용 헬퍼 메소드 (코드 중복 제거)
    private void insertTransactionHistory(String acctNo, String summary, BigDecimal amount, int type) {
        // 사용자 이름 가져오기
        String custName = "";
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            custName = userDetails.getCustName();
        }

        CustTranHistDTO histDTO = new CustTranHistDTO();
        histDTO.setTranAcctNo(acctNo);
        histDTO.setTranCustName(summary); // 적요
        histDTO.setTranType(type);        // 1:입금, 2:출금
        histDTO.setTranAmount(amount.intValue()); // 원화 금액이므로 int 변환 가능

        mypageMapper.insertTranHist(histDTO);
    }

    // 환전하기 전 계좌 비밀번호 일치하는지 확인
    public boolean checkAccountPassword(String acctNo, String acctPass) {

        CustAcctDTO custAcctDTO = mypageMapper.selectCustAcct(acctNo);
        if (custAcctDTO == null) {
            return false;
        }

        return passwordEncoder.matches(acctPass, custAcctDTO.getAcctPw());
    }

}
