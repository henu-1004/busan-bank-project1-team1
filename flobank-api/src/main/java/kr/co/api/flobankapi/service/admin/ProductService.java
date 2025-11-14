package kr.co.api.flobankapi.service.admin;

import kr.co.api.flobankapi.dto.ProductDTO;
import kr.co.api.flobankapi.dto.ProductLimitDTO;
import kr.co.api.flobankapi.mapper.admin.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ProductService {

    private final ProductMapper productMapper;

    @Transactional
    public void insertProduct(ProductDTO dto, List<ProductLimitDTO> limits) {

        // 1) 상품 기본정보 INSERT (dpst_id는 트리거가 생성)
        productMapper.insertProduct(dto);

        // 2) 생성된 dpst_id 조회
        String dpstId = productMapper.getRecentDpstId ();

        // 3) Limit 리스트에 dpst_id 주입
        for (ProductLimitDTO limit : limits) {
            limit.setLmtDpstId(dpstId);
        }

        // 4) 통화별 한 번에 INSERT
        productMapper.insertProductLimits(limits);
    }


}