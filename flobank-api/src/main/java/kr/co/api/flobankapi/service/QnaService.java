package kr.co.api.flobankapi.service;

import kr.co.api.flobankapi.dto.QnaDTO;
import kr.co.api.flobankapi.mapper.QnaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QnaService {

    private final QnaMapper qnaMapper;
    private static final int PAGE_SIZE = 6;

    public Map<String, Object> getQnaPage(int page) {
        int totalCount = qnaMapper.countQna();
        int totalPage = (int) Math.ceil(totalCount / (double) PAGE_SIZE);
        if (totalPage < 1) totalPage = 1;

        int start = (page - 1) * PAGE_SIZE + 1;
        int end = page * PAGE_SIZE;

        List<QnaDTO> list = qnaMapper.selectQnaPage(start, end);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("page", page);
        result.put("pageSize", PAGE_SIZE);
        result.put("totalPage", totalPage);
        result.put("totalCount", totalCount);

        return result;
    }

    @Transactional
    public QnaDTO getQna(Long qnaNo) {
        qnaMapper.updateQnaViewCnt(qnaNo);
        return qnaMapper.selectQnaByNo(qnaNo);
    }

    public QnaDTO findQna(Long qnaNo) {
        return qnaMapper.selectQnaByNo(qnaNo);
    }

    public void createQna(QnaDTO qna) {
        qnaMapper.insertQna(qna);
    }

    public void updateQna(QnaDTO qna) {
        qnaMapper.updateQna(qna);
    }

    public void deleteQna(Long qnaNo) {
        qnaMapper.deleteQna(qnaNo);
    }
}
