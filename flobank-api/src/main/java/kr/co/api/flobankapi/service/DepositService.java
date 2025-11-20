package kr.co.api.flobankapi.service;

import kr.co.api.flobankapi.dto.*;
import kr.co.api.flobankapi.mapper.DepositMapper;
import kr.co.api.flobankapi.mapper.admin.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DepositService {
    private final DepositMapper depositMapper;

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
        if (productDTO.getDpstPeriodType().equals("고정형")) {
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

    public ProductDTO getProduct(String dpstId) {
        return depositMapper.findProductById(dpstId);

    }

    public List<CustAcctDTO> getAcctList(String acctCustCode) {
        return depositMapper.getKRWAccts(acctCustCode);
    }

    public CustFrgnAcctDTO getFrgnAcct(String frgnAcctCustCode) {
        return depositMapper.getFrgnAcct(frgnAcctCustCode);
    }

    public List<FrgnAcctBalanceDTO> getFrgnAcctBalList(String balFrgnAcctNo) {
        return depositMapper.getFrgnAcctBalList(balFrgnAcctNo);
    }

    public int getActiveProductCount() {
        return depositMapper.countActiveProducts();
    }
}


