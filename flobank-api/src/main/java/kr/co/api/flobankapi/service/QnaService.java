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

    public Map<String, Object> getQnaPage(int page) {

        int pageSize = 10;

        int totalCount = qnaMapper.countQna();
        int totalPage = (int) Math.ceil(totalCount / (double) pageSize);
        if (totalPage < 1) totalPage = 1;

        if (page < 1) {
            page = 1;
        } else if (page > totalPage) {
            page = totalPage;
        }

        int start = (page - 1) * pageSize + 1;
        int end = page * pageSize;

        List<QnaDTO> list = qnaMapper.selectQnaPage(start, end);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("totalPage", totalPage);
        result.put("totalCount", totalCount);

        return result;
    }

    @Transactional
    public QnaDTO getQna(Long qnaNo) {
        qnaMapper.updateQnaHit(qnaNo);
        return qnaMapper.selectQnaByNo(qnaNo);
    }

    public void insertQna(QnaDTO qnaDTO) {
        qnaMapper.insertQna(qnaDTO);
    }
}
