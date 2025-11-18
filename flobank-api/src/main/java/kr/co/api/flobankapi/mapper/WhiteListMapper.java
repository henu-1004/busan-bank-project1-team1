package kr.co.api.flobankapi.mapper;

import kr.co.api.flobankapi.dto.ProductDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface WhiteListMapper {
    public List<ProductDTO> dpstAllInfo(String dpstId);
    public List<String> dpstIdList();

}
