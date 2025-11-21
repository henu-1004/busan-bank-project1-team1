package kr.co.api.flobankapi.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.api.flobankapi.dto.*;
import kr.co.api.flobankapi.mapper.DepositMapper;
import kr.co.api.flobankapi.mapper.admin.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DepositService {
    private final DepositMapper depositMapper;
    private final RateService rateService;
    private final ObjectMapper objectMapper;

    public List<ProductDTO> getActiveProducts() {
        return depositMapper.findActiveProducts();
    }
    public ProductDTO selectDpstProduct(String dpstId) {
        ProductDTO productDTO = depositMapper.selectDpstProduct(dpstId);
        
        // 적용 가능 통화 목록
        List<String> currencyList = Arrays.stream(productDTO.getDpstCurrency().split(","))
                .map(String::trim)       // 공백 제거
                .filter(s -> !s.isEmpty()) // 빈 문자열 제거
                .collect(Collectors.toList());
        List<CurrencyInfoDTO> curDtoList = new ArrayList<>();

        for (String currency : currencyList) {
            CurrencyInfoDTO curDto = depositMapper.getCurrency(currency);
            curDtoList.add(curDto);
        }
        productDTO.setDpstCurrencyList(currencyList);
        productDTO.setDpstCurrencyDtoList(curDtoList);

        // 통화별 가입 금액 제한
        if (productDTO.getDpstMinYn().equals("Y") || productDTO.getDpstMaxYn().equals("Y")) {
            List<ProductLimitDTO> limits = depositMapper.limitOfDpst(dpstId);
            productDTO.setLimits(limits);
        }

        // 상품 가입 기간
        if (productDTO.getDpstPeriodType()==2) {
            List<ProductPeriodDTO> dtoList = depositMapper.dpstFixedPeriods(dpstId);
            List<Integer> periodList = new ArrayList<>();
            for (ProductPeriodDTO dto : dtoList) {
                periodList.add(dto.getFixedMonth());
            }
            productDTO.setPeriodFixedMonthList(periodList);

        } else {
            ProductPeriodDTO dto = depositMapper.dpstMinMaxPeriod(dpstId);
            productDTO.setPeriodMinMonth(dto.getMinMonth());
            productDTO.setPeriodMaxMonth(dto.getMaxMonth());
        }

        // 분할 인출 규정
        if (productDTO.getDpstPartWdrwYn().equals("Y")) {
            ProductWithdrawRuleDTO dto = depositMapper.getDpstWdrwInfo(dpstId);
            List<ProductWithdrawAmtDTO> list = depositMapper.getDpstAmtList(dpstId);
            productDTO.setWdrwMinMonth(dto.getMinMonths());
            productDTO.setWdrwMax(dto.getMaxCount());
            productDTO.setWithdrawMinAmtList(list);
        }
        return productDTO;
    }

    public DepositExchangeDTO exchangeCalc(String currency) {

        try {
            String jsonResponse = rateService.getRate(LocalDate.now().toString());
            if (jsonResponse == null) {
                throw new RuntimeException("환율 데이터를 가져올 수 없습니다.");
            }

            // 2. JSON 문자열을 DTO 리스트로 변환
            List<RateInfoDTO> rateList = objectMapper.readValue(jsonResponse, new TypeReference<List<RateInfoDTO>>() {});

            log.info(rateList.toString());

            // 3. 리스트에서 내가 원하는 통화(예: USD) 찾기
            RateInfoDTO targetRateInfo = rateList.stream()
                    .filter(rate -> rate.getCurUnit().contains(currency))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("해당 통화의 환율 정보를 찾을 수 없습니다: " + currency));

            // 4. 환율 문자열(예: "1,350.50")에서 쉼표 제거 후 숫자로 변환
            String baseRate = targetRateInfo.getDealBasR().replace(",", "");
            String tts = targetRateInfo.getTts().replace(",", "");

            BigDecimal exchangeRate = new BigDecimal(baseRate);
            BigDecimal ttsRate = new BigDecimal(tts);
            int prefRate = depositMapper.getPrefRate(currency);


            BigDecimal spreadHalfPref = ttsRate.subtract(exchangeRate)
                    .multiply(BigDecimal.valueOf(100 - prefRate))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            BigDecimal appliedRate = exchangeRate.add(spreadHalfPref);

            DepositExchangeDTO exchangeDTO = new DepositExchangeDTO();
            exchangeDTO.setAppliedRate(appliedRate);
            exchangeDTO.setPrefRate(prefRate);
            exchangeDTO.setBaseRate(exchangeRate);
            exchangeDTO.setTtsRate(ttsRate);
            exchangeDTO.setSpreadHalfPref(spreadHalfPref);
            return exchangeDTO;
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException("환율 계산 중 오류 발생", e);
        }
    }

    public ProductDTO getProduct(String dpstId) {
        return depositMapper.findProductById(dpstId);

    }

    public List<CustAcctDTO> getAcctList(String acctCustCode) {
        return depositMapper.getKRWAccts(acctCustCode);
    }

    public CustFrgnAcctDTO getFrgnAcct(String frgnAcctCustCode) {
        return depositMapper.getFrgnAcct(frgnAcctCustCode);
    }



    public String getTermsFileByTitle(String productName) {
        return depositMapper.findTermsFileByTitle(productName);
    }

    public List<FrgnAcctBalanceDTO> getFrgnAcctBalList(String balFrgnAcctNo) {
        return depositMapper.getFrgnAcctBalList(balFrgnAcctNo);

    }

    public int getActiveProductCount() {
        return depositMapper.countActiveProducts();
    }


    public List<DepositRateDTO> getRatesByBaseDate(Date baseDate) {
        log.info("기준일({})의 금리 조회 요청", baseDate);
        return depositMapper.findRatesByBaseDate(baseDate);
    }

}


