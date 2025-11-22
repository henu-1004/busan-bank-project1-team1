package kr.co.api.flobankapi.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.api.flobankapi.dto.*;
import kr.co.api.flobankapi.mapper.DepositMapper;
import kr.co.api.flobankapi.mapper.admin.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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


    private String getValidRateJson() {
        LocalDate date = LocalDate.now().minusDays(1);

        for (int i = 0; i < 7; i++) {
            String response = rateService.getRate(date.toString());
            log.warn("📌 [{}] raw response = {}", date, response);

            // JSON 배열 여부 검증
            try {
                List<?> testList = objectMapper.readValue(response, new TypeReference<List<Object>>() {});
                if (testList != null && !testList.isEmpty()) {
                    log.warn("📌 사용된 영업일 환율 날짜 = {}", date);
                    return response;
                }
            } catch (Exception e) {
                log.error("🚨 JSON 파싱 실패 ({}): {}", date, response);
            }

            log.warn("⚠ {} 날짜 환율 미존재 → 하루 더 이전으로 이동", date);
            date = date.minusDays(1);
        }

        throw new RuntimeException("7일 이내 영업일 환율 정보를 찾을 수 없습니다.");
    }

    public DepositExchangeDTO exchangeCalc(String currency) {

        try {
            String jsonResponse =  getValidRateJson();
            System.err.println("📌 Raw API Date: " + LocalDate.now().minusDays(2).toString());
            log.error("📌 Raw API response: {}", jsonResponse);



            // 2. JSON 문자열을 DTO 리스트로 변환
            List<RateInfoDTO> rateList = objectMapper.readValue(jsonResponse, new TypeReference<List<RateInfoDTO>>() {});

            log.error("📌 Parsed rateList Size: {}", rateList.size());
            log.error("📌 Parsed rateList Value: {}", rateList);

            boolean exists = rateList.stream()
                    .anyMatch(rate -> {
                        String curUnit = rate.getCurUnit().replace("(100)", "").trim();
                        String input = currency.trim();
                        return curUnit.equalsIgnoreCase(input);
                    });

            if (!exists) {
                log.error("🚨 '{}' 통화 없음. 전체 리스트 {}", currency, rateList);
                throw new IllegalArgumentException("해당 통화의 환율 정보를 찾을 수 없습니다: " + currency);
            }



            // 3. 리스트에서 내가 원하는 통화(예: USD) 찾기
            RateInfoDTO targetRateInfo = rateList.stream()
                    .filter(rate -> rate.getCurUnit().replace("(100)", "").trim().equalsIgnoreCase(currency.trim()))
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

    public String getKAcctPw(String acctNo){
        return depositMapper.getAcctPw(acctNo);
    }

    public String getFAcctPw(String frgnAcctNo){
        return depositMapper.getFrgnAcctPw(frgnAcctNo);
    }

    public InterestRateDTO getRecentInterest(String currency) {
        return depositMapper.getRecentInterest(currency);
    }

    public DpstAcctHdrDTO insertAndGetDpstAcct(DpstAcctHdrDTO hdrDTO){
        depositMapper.insertDpstAcctHdr(hdrDTO);
        return depositMapper.selectInsertedAcct(hdrDTO.getDpstHdrCustCode(), hdrDTO.getDpstHdrDpstId());
    }

    public void insertDpstDtl(DpstAcctDtlDTO dpstAcctDtlDTO) {
        depositMapper.insertDpstAcctDtl(dpstAcctDtlDTO);
    }

    public void insertCustTranHist(CustTranHistDTO custTranHistDTO) {
        depositMapper.insertCustTranHist(custTranHistDTO);
    }

    @Transactional
    public DpstAcctHdrDTO openDepositAcctTransaction(
            DpstAcctHdrDTO hdrDTO,
            DpstAcctDtlDTO dtlDTO,
            CustTranHistDTO custTranDTO,
            String withdrawType
    ) {
        // 예금 헤더 insert
        depositMapper.insertDpstAcctHdr(hdrDTO);

        DpstAcctHdrDTO insdto = depositMapper.selectInsertedAcct(hdrDTO.getDpstHdrCustCode(), hdrDTO.getDpstHdrDpstId());


        // 예금 거래내역 insert
        dtlDTO.setDpstDtlHdrNo(insdto.getDpstHdrAcctNo());
        depositMapper.insertDpstAcctDtl(dtlDTO);


        // 고객 거래내역 insert
        custTranDTO.setTranAcctNo(insdto.getDpstHdrLinkedAcctNo());
        custTranDTO.setTranRecAcctNo(insdto.getDpstHdrAcctNo());
        depositMapper.insertCustTranHist(custTranDTO);


        // 계좌 잔액 업데이트
        String acctNo = insdto.getDpstHdrLinkedAcctNo();
        if (withdrawType.equals("krw")){
            CustAcctDTO krwAcct = new CustAcctDTO();
            krwAcct.setAcctNo(acctNo);
            krwAcct.setAcctBalance(depositMapper.selectKrwAcctBalance(acctNo).getAcctBalance()-dtlDTO.getDpstDtlAmount().setScale(0, RoundingMode.HALF_UP).intValue());
            depositMapper.updateAcctBalance(krwAcct);
        }else {
            FrgnAcctBalanceDTO frgnAcct = new FrgnAcctBalanceDTO();
            frgnAcct.setBalNo(acctNo);
            frgnAcct.setBalBalance(depositMapper.selectFrgnAcctBalance(acctNo).getBalBalance()-dtlDTO.getDpstDtlAmount().setScale(0, RoundingMode.HALF_UP).intValue());
            depositMapper.updateBalBalance(frgnAcct);
        }


        // 4. 방금 생성된 예금계좌 다시 조회
        return insdto;
    }

}


