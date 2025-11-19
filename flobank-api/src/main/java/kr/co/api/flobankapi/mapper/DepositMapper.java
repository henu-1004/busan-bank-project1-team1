package kr.co.api.flobankapi.mapper;

import kr.co.api.flobankapi.dto.ProductDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DepositMapper {

    List<ProductDTO> findActiveProducts();

    int countActiveProducts();
}
