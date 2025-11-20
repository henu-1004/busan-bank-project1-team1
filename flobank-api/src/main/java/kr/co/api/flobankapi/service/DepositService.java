package kr.co.api.flobankapi.service;

import kr.co.api.flobankapi.dto.ProductDTO;
import kr.co.api.flobankapi.mapper.DepositMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepositService {

    private final DepositMapper depositMapper;

    public List<ProductDTO> getActiveProducts() {
        return depositMapper.findActiveProducts();
    }

    public int getActiveProductCount() {
        return depositMapper.countActiveProducts();
    }

    public ProductDTO getProduct(String dpstId) {
        return depositMapper.findProductById(dpstId);
    }



}

