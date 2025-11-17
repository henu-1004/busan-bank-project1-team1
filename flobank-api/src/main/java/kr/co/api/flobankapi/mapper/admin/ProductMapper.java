package kr.co.api.flobankapi.mapper.admin;


import kr.co.api.flobankapi.dto.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductMapper {

    // 상품 정보 INSERT
    int insertProduct(ProductDTO dto);

    // 통화별 최소/최대 INSERT
    int insertProductLimits(
            @Param("dpstId") String dpstId,
            @Param("limits") List<ProductLimitDTO> limits
    );


    // ★ 트리거가 생성한 가장 최근 상품 ID 조회 (필수)
    String getRecentDpstId();

    // 가입 기간 저장
    void insertProductPeriods(List<ProductPeriodDTO> periods);

    // 분할 인출
    void insertWithdrawalRule(ProductWithdrawRuleDTO rule);

    // 통화별 최소 출금 금액
    void insertWithdrawalAmounts(List<ProductWithdrawAmtDTO> amts);

    //  상태 업데이트
    void updateStatus(@Param("dpstId") String dpstId, @Param("status") int status);

    //  상태별 상품 리스트 가져오기
    List<ProductDTO> getProductsByStatus(@Param("status") int status);


    ProductDTO getProductById(@Param("dpstId") String dpstId);



    List<ProductPeriodDTO> getPeriods(String dpstId);

    ProductWithdrawRuleDTO getWithdrawRule(String dpstId);

    List<ProductWithdrawAmtDTO> getWithdrawAmts(String dpstId);

    List<ProductLimitDTO> getLimits(String dpstId);





}
