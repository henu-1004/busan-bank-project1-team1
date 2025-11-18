package kr.co.api.flobankapi.service.admin;

import kr.co.api.flobankapi.dto.*;
import kr.co.api.flobankapi.mapper.admin.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@RequiredArgsConstructor
@Service
public class ProductService {

    private final ProductMapper productMapper;

    @Transactional
    public void insertProduct(ProductDTO dto,
                              List<ProductLimitDTO> limits,
                              List<ProductPeriodDTO> periods,
                              ProductWithdrawRuleDTO wdrwInfo,
                              List<ProductWithdrawAmtDTO> withdrawAmts) {

            // 1) 기본 상품 INSERT
            productMapper.insertProduct(dto);
            String dpstId = dto.getDpstId();


            // 2) 최소/최대 금액 정리
            if (limits != null && !limits.isEmpty()) {
                List<ProductLimitDTO> clean = new ArrayList<>();
                Set<String> seen = new HashSet<>();

                for (ProductLimitDTO l : limits) {
                    if (l.getLmtCurrency() == null || l.getLmtCurrency().trim().isEmpty()) continue;
                    if (seen.add(l.getLmtCurrency())) clean.add(l);
                }

                limits = clean;
            }


            // 3) 최소/최대 금액 INSERT
            if (dto.getDpstType() == 1 && limits != null && !limits.isEmpty()) {

                Map<String, Object> map = new HashMap<>();
                map.put("dpstId", dpstId);
                map.put("limits", limits);

                productMapper.insertProductLimits(map);
            }


            // 4) 가입기간 INSERT
            if (periods != null && !periods.isEmpty()) {

                Map<String, Object> map2 = new HashMap<>();
                map2.put("dpstId", dpstId);
                map2.put("list", periods);

                productMapper.insertProductPeriods(map2);
            }


            // 5) 분할 인출 규정 INSERT
            if (wdrwInfo != null) {
                wdrwInfo.setDpstId(dpstId);
                productMapper.insertWithdrawalRule(wdrwInfo);
            }


            // 6) 통화별 최소 출금금액 INSERT
            if (withdrawAmts != null && !withdrawAmts.isEmpty()) {

                Map<String, Object> map3 = new HashMap<>();
                map3.put("dpstId", dpstId);
                map3.put("list", withdrawAmts);

                productMapper.insertWithdrawalAmounts(map3);
            }
        }



    public List<ProductDTO> getProductsByStatus(int status) {
        return productMapper.getProductsByStatus(status);
    }


    public void updateStatus(String dpstId, int status) {
        productMapper.updateStatus(dpstId, status);
    }


    public ProductDTO getProductById(String dpstId) {
        return productMapper.getProductById(dpstId);
    }



    public List<ProductPeriodDTO> getPeriods(String dpstId) {
        return productMapper.getPeriods(dpstId);
    }


    public ProductWithdrawRuleDTO getWithdrawRule(String dpstId) {
        return productMapper.getWithdrawRule(dpstId);
    }


    public List<ProductWithdrawAmtDTO> getWithdrawAmts(String dpstId) {
        return productMapper.getWithdrawAmts(dpstId);
    }


    public List<ProductLimitDTO> getLimits(String dpstId) {
        return productMapper.getLimits(dpstId);
    }


    // 상품 자동 업데이트
    public void updateOpenedProducts() {
        productMapper.updateStatusToOpened();
    }





}