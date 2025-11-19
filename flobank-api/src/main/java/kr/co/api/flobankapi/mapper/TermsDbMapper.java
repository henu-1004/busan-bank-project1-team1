package kr.co.api.flobankapi.mapper;

import kr.co.api.flobankapi.dto.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TermsDbMapper {

    List<TermsHistDTO> selectTermsByCate(@Param("cate") int cate);

    int insertTermsMaster(TermsMasterDTO dto);

    // 최신 order 조회
    Integer selectMaxOrderByCate(@Param("cate") int cate);

    // HIST insert
    void insertTermsHist(TermsHistDTO dto);

    // 전체 + 최신 버전 조회
    List<TermsMasterDTO> selectAllTermsWithLatestVersion();

    // 최신 버전 내용 조회
    TermsHistDTO selectLatestHist(
            @Param("termCate") int termCate,
            @Param("termOrder") int termOrder
    );

    // MASTER 수정
    void updateTermsMaster(TermsMasterDTO dto);

    // MASTER 삭제
    void deleteTerms(
            @Param("cate") int cate,
            @Param("order") int order
    );

    // HIST 삭제
    void deleteTermsHist(
            @Param("cate") int cate,
            @Param("order") int order
    );

    // 고객 약관 동의 저장
    void saveAgreeHist(TermsAgreeDTO dto);


    /** ★★★★★ 여기 2개가 없어서 오류 난 것 ★★★★★ */
    List<TermsMasterDTO> selectTermsPage(
            @Param("start") int start,
            @Param("pageSize") int pageSize
    );



    int countTermsFiltered(
            @Param("type") String type,
            @Param("keyword") String keyword
    );

    int countTerms();


    List<TermsMasterDTO> selectTermsPageFiltered(
            @Param("start") int start,
            @Param("pageSize") int pageSize,
            @Param("type") String type,
            @Param("keyword") String keyword
    );

    TermsMasterDTO selectMaster(@Param("cate") int cate, @Param("order") int order);





}
