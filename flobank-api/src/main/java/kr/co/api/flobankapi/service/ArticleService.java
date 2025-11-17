package kr.co.api.flobankapi.service;

import kr.co.api.flobankapi.dto.ArticleDTO;
import kr.co.api.flobankapi.mapper.ArticleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleMapper articleMapper;

    private static final int PAGE_SIZE = 9;

    public Map<String, Object> getArticlePage(String mode, int page) {
        if (page < 1) {
            page = 1;
        }
        LocalDateTime fromDate = resolveFromDate(mode);

        int totalCount = articleMapper.countArticles(fromDate);
        int totalPage = (int) Math.ceil(totalCount / (double) PAGE_SIZE);
        if (totalPage < 1) {
            totalPage = 1;
        }

        int start = (page - 1) * PAGE_SIZE + 1;
        int end = page * PAGE_SIZE;

        List<ArticleDTO> list = articleMapper.selectArticlePage(start, end, fromDate);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("page", page);
        result.put("pageSize", PAGE_SIZE);
        result.put("totalPage", totalPage);
        result.put("totalCount", totalCount);
        return result;
    }

    private LocalDateTime resolveFromDate(String mode) {
        LocalDate today = LocalDate.now();

        if ("recent5".equals(mode)) {
            return today.minusDays(4).atStartOfDay();
        }

        return today.atStartOfDay();
    }
}
