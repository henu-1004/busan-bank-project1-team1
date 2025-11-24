package kr.co.api.flobankapi.mapper;

import kr.co.api.flobankapi.document.*;
import kr.co.api.flobankapi.dto.search.SearchLogDTO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface SearchDataMapper {

    // 1. 상품 (TB_DPST_PROD_INFO)
    // 이미지 기준: dpst_id, dpst_name, dpst_info, dpst_descript, dpst_reg_dt
    @Select("""
    SELECT 
        dpst_id AS dpstId,
        dpst_name AS dpstName,
        dpst_info AS dpstInfo,
        dpst_descript AS dpstDescript
    FROM TB_DPST_PROD_INFO
    WHERE DPST_STATUS = 3
""")
    List<ProductDocument> selectAllProducts();

    // 2. FAQ (TB_FAQ_HDR)
    // 이미지 기준: faq_no, faq_question, faq_answer
    @Select("SELECT faq_no as faqNo, faq_question as faqQuestion, faq_answer as faqAnswer FROM TB_FAQ_HDR")
    List<FaqDocument> selectAllFaqs();

    // 3. 약관 (TB_TERMS_MASTER)
    // 이미지 기준: term_order(PK), term_title, term_reg_dy
    // 주의: '내용(content)' 컬럼이 테이블에 없으므로 제목만 검색되도록 매핑하거나, content에는 빈 문자열을 넣습니다.
    @Select("""
        SELECT 
            h.thist_no        AS thistNo,
            m.term_title      AS termTitle,
            h.thist_content   AS thistContent,
            h.thist_version   AS thistVersion,
            h.thist_file      AS thistFile,
            TO_DATE(h.thist_reg_dy, 'YYYYMMDD') AS thistRegDy
        FROM TB_TERMS_HIST h
        JOIN TB_TERMS_MASTER m 
          ON h.thist_term_order = m.term_order 
         AND h.thist_term_cate  = m.term_cate
    """)
    List<TermDocument> selectAllTerms();


    // 4. 공지사항 (TB_BOARD_HDR where board_type = 1)
    // 이미지 기준: board_no, board_title, board_content, board_reg_dt
    @Select("SELECT board_no as boardNo, board_title as boardTitle, board_content as boardContent, board_reg_dt as boardRegDt " +
            "FROM TB_BOARD_HDR " +
            "WHERE board_type = 1")
    List<NoticeDocument> selectAllNotices();

    // 5. 이벤트 (TB_BOARD_HDR where board_type = 2)
    // 이미지 기준: board_no, board_title, board_content, board_reg_dt
    // 주의: 이미지상에 '혜택(benefit)' 컬럼이 안 보입니다. 내용(content)을 혜택 필드에도 같이 넣어주거나 비워둡니다.
    @Select("SELECT board_no as boardNo, board_title as boardTitle, board_content as boardContent, board_content as eventBenefit, board_reg_dt as boardRegDt " +
            "FROM TB_BOARD_HDR " +
            "WHERE board_type = 2")
    List<EventDocument> selectAllEvents();


    // 1. [저장] 토큰 저장 (모든 검색어 - 인기검색어용)
    @Insert("INSERT INTO TB_SEARCH_TOKEN (tok_no, tok_txt) VALUES (SEQ_SEARCH_TOKEN.NEXTVAL, #{keyword})")
    void insertSearchToken(String keyword);

    // 2. [저장] 내 검색 기록 저장 (로그인 시)
    @Insert("INSERT INTO TB_SEARCH_LOG (search_no, search_txt, search_cust_code, search_reg_dt) VALUES (SEQ_SEARCH_LOG.NEXTVAL, #{keyword}, #{custCode}, SYSDATE)")
    void insertSearchLog(String keyword, String custCode);

    // 3. [조회] 인기 검색어 TOP 10 (많이 검색된 순)
    @Select("""
        SELECT * FROM (
            SELECT tok_txt as keyword, COUNT(*) as count
            FROM TB_SEARCH_TOKEN
            WHERE tok_txt IS NOT NULL
            GROUP BY tok_txt
            ORDER BY COUNT(*) DESC
        ) WHERE ROWNUM <= 10
    """)
    List<SearchLogDTO> selectPopularKeywords();

    // 4. [조회] 내 최근 검색어 5개
    @Select("""
        SELECT search_txt as keyword, TO_CHAR(search_reg_dt, 'MM.DD') as date
        FROM (
            SELECT search_txt, search_reg_dt
            FROM TB_SEARCH_LOG
            WHERE search_cust_code = #{custCode}
            ORDER BY search_no DESC
        ) WHERE ROWNUM <= 5
    """)
    List<SearchLogDTO> selectRecentKeywords(String custCode);

}