package kr.co.api.flobankapi.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.api.flobankapi.dto.CustAcctDTO;
import kr.co.api.flobankapi.dto.RateInfoDTO;
import kr.co.api.flobankapi.mapper.MypageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

}
