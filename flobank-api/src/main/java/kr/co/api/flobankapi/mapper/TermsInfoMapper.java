package kr.co.api.flobankapi.mapper;

import kr.co.api.flobankapi.dto.TermsDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


@Mapper
public interface TermsInfoMapper {

    List<TermsDTO> findByType(@Param("termType") int termType);
}
