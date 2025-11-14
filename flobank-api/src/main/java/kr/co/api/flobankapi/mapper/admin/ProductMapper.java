package kr.co.api.flobankapi.mapper.admin;


import kr.co.api.flobankapi.dto.ProductDTO;
import kr.co.api.flobankapi.dto.ProductLimitDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ProductMapper {

    // 상품 정보 INSERT
    int insertProduct(ProductDTO dto);

    // 통화별 최소/최대 INSERT
    int insertProductLimits(List<ProductLimitDTO> limits);

    // ★ 트리거가 생성한 가장 최근 상품 ID 조회 (필수)
    String getRecentDpstId();
}
