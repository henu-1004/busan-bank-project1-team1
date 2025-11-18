package kr.co.api.flobankapi.mapper;

import kr.co.api.flobankapi.dto.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TermsDbMapper {

    int insertTermsMaster(TermsMasterDTO dto);

    Integer selectMaxOrderByCate(@Param("cate") int cate);

    int insertTermsHist(TermsHistDTO dto);

    List<TermsMasterDTO> selectAllTermsWithLatestVersion();

    TermsHistDTO selectLatestHist(
            @Param("cate") int termCate,
            @Param("order") int termOrder
    );

    int updateTermsMaster(TermsMasterDTO dto);

    int deleteTerms(
            @Param("cate") int cate,
            @Param("order") int order
    );

    int deleteTermsHist(
            @Param("cate") int cate,
            @Param("order") int order
    );

    int saveAgreeHist(TermsAgreeDTO dto);
}
