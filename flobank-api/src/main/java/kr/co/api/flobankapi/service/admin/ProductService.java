package kr.co.api.flobankapi.service.admin;

import kr.co.api.flobankapi.dto.*;
import kr.co.api.flobankapi.mapper.admin.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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


        String dpstId = dto.getDpstId();   // 트리거+selectKey로 셋팅된 값!


        // 최소/최대 금액 리스트 보정
        if (limits != null && !limits.isEmpty()) {

            List<ProductLimitDTO> clean = new ArrayList<>();
            Set<String> seen = new HashSet<>();

            for (ProductLimitDTO l : limits) {

                // 빈 통화 제거
                if (l.getLmtCurrency() == null || l.getLmtCurrency().trim().isEmpty()) {
                    continue;
                }

                // 중복 통화 제거
                if (seen.add(l.getLmtCurrency())) {
                    clean.add(l);
                }
            }

            //  removeIf 쓰지 말고 새로운 리스트로 완전 교체
            limits = clean;
        }

        // 3) 최소/최대 금액 INSERT
        if (dto.getDpstType() == 1 && !limits.isEmpty()) {

            for (ProductLimitDTO limit : limits) {
                limit.setLmtDpstId(dpstId);
            }
            productMapper.insertProductLimits(dpstId, limits);
        }

        // 4) 가입 기간 INSERT
        for (ProductPeriodDTO p : periods) {
            p.setDpstId(dpstId);
        }
        productMapper.insertProductPeriods(periods);

        // 5) 분할 인출 규정
        if (wdrwInfo != null) {
            wdrwInfo.setDpstId(dpstId);
            productMapper.insertWithdrawalRule(wdrwInfo);
        }

        // 6) 통화별 최소 출금금액
        for (ProductWithdrawAmtDTO amt : withdrawAmts) {
            amt.setDpstId(dpstId);
        }
        if (!withdrawAmts.isEmpty()) {
            productMapper.insertWithdrawalAmounts(withdrawAmts);
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








}