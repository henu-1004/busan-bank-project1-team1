package kr.co.api.flobankapi.mapper;

import kr.co.api.flobankapi.dto.QnaDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QnaMapper {

    int countQna();

    List<QnaDTO> selectQnaPage(@Param("start") int start,
                               @Param("end") int end);

    QnaDTO selectQnaByNo(@Param("qnaNo") Long qnaNo);

    void updateQnaViewCnt(@Param("qnaNo") Long qnaNo);

    void insertQna(QnaDTO qna);

    void updateQna(QnaDTO qna);

    void deleteQna(@Param("qnaNo") Long qnaNo);
}
