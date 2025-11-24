package kr.co.api.flobankapi.service;

import kr.co.api.flobankapi.document.*;
import kr.co.api.flobankapi.dto.search.SearchLogDTO;
import kr.co.api.flobankapi.dto.search.SearchResultItemDTO;
import kr.co.api.flobankapi.dto.search.SearchResultResponseDTO;
import kr.co.api.flobankapi.dto.search.SectionResultDTO;
import kr.co.api.flobankapi.mapper.SearchMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final SearchMapper searchMapper;

    private static final int PREVIEW_SIZE = 4;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    // ======================================================================
    //  검색어 저장
    // ======================================================================
    public void saveSearchKeyword(String keyword, String custCode) {
        if (keyword == null || keyword.trim().isEmpty()) return;

        // 1. 인기 검색어 저장
        searchMapper.insertSearchToken(keyword.trim());

        // 2. 로그인 사용자만 최근 검색어 저장
        if (custCode != null && !custCode.equals("ANONYMOUS") && !custCode.equals("null")) {

            // 기존 검색어 삭제 (중복 방지)
            searchMapper.deleteDuplicateSearchLog(keyword.trim(), custCode);

            // 최신 검색어로 INSERT (한 줄만 유지)
            searchMapper.insertSearchLog(keyword.trim(), custCode);
        }
    }

    // ======================================================================
    // 검색어 조회
    // ======================================================================
    public List<SearchLogDTO> getPopularKeywords() {
        return searchMapper.selectPopularKeywords();
    }

    public List<SearchLogDTO> getRecentKeywords(String custCode) {
        return searchMapper.selectRecentKeywords(custCode);
    }

    // ======================================================================
    //  1. 통합 검색 미리보기
    // ======================================================================
    public SearchResultResponseDTO integratedSearchPreview(String keyword) {
        SearchResultResponseDTO response = new SearchResultResponseDTO();
        response.setSections(new HashMap<>());

        if (keyword == null || keyword.trim().isEmpty()) return response;

        List<Query> queries = new ArrayList<>();
        List<Class<?>> classes = new ArrayList<>();
        List<String> tabKeys = new ArrayList<>();

        // 1) 상품
        queries.add(buildNativeQuery(keyword, PREVIEW_SIZE, "dpstName^3", "dpstInfo", "dpstDescript"));
        classes.add(ProductDocument.class);
        tabKeys.add("product");

        // 2) FAQ
        queries.add(buildNativeQuery(keyword, PREVIEW_SIZE, "faqQuestion^3", "faqAnswer"));
        classes.add(FaqDocument.class);
        tabKeys.add("faq");

        // 3) 약관
        queries.add(buildNativeQuery(keyword, PREVIEW_SIZE, "termTitle^3", "thistContent"));
        classes.add(TermDocument.class);
        tabKeys.add("docs");

        // 4) 공지
        queries.add(buildNativeQuery(keyword, PREVIEW_SIZE, "boardTitle^3", "boardContent"));
        classes.add(NoticeDocument.class);
        tabKeys.add("notice");

        // 5) 이벤트
        queries.add(buildNativeQuery(keyword, PREVIEW_SIZE, "boardTitle^3", "boardContent", "eventBenefit"));
        classes.add(EventDocument.class);
        tabKeys.add("event");

        List<SearchHits<?>> multiHits = elasticsearchOperations.multiSearch(queries, classes);
        long totalCount = 0;

        for (int i = 0; i < multiHits.size(); i++) {
            SearchHits<?> hits = multiHits.get(i);
            String key = tabKeys.get(i);

            SectionResultDTO sectionDTO = mapHitsToSection(hits, key);
            response.getSections().put(key, sectionDTO);
            totalCount += hits.getTotalHits();
        }
        response.setTotalCount(totalCount);
        return response;
    }

    // ======================================================================
    //  2. 탭별 상세 검색
    // ======================================================================
    public SearchResultResponseDTO tabSearch(String keyword, String type, int page) {
        SearchResultResponseDTO response = new SearchResultResponseDTO();
        if (keyword == null || type == null) return response;

        Pageable pageable = PageRequest.of(page, 10);
        Query query;
        Class<?> docClass;

        switch (type) {
            case "product":
                query = buildNativeQuery(keyword, pageable, "dpstName^3", "dpstInfo", "dpstDescript");
                docClass = ProductDocument.class;
                break;
            case "faq":
                query = buildNativeQuery(keyword, pageable, "faqQuestion^3", "faqAnswer");
                docClass = FaqDocument.class;
                break;
            case "docs":
                query = buildNativeQuery(keyword, pageable, "termTitle^3", "thistContent");
                docClass = TermDocument.class;
                break;
            case "notice":
                query = buildNativeQuery(keyword, pageable, "boardTitle^3", "boardContent");
                docClass = NoticeDocument.class;
                break;
            case "event":
                query = buildNativeQuery(keyword, pageable, "boardTitle^3", "boardContent", "eventBenefit");
                docClass = EventDocument.class;
                break;
            default:
                return response;
        }

        SearchHits<?> hits = elasticsearchOperations.search(query, docClass);
        SectionResultDTO section = mapHitsToSection(hits, type);
        response.setSections(Map.of(type, section));
        response.setTotalCount(hits.getTotalHits());
        return response;
    }

    // ======================================================================
    // 내부 헬퍼 메서드
    // ======================================================================
    private NativeQuery buildNativeQuery(String keyword, int size, String... fields) {
        // "예금" -> "*예금*" 으로 변환하여 부분 일치 유도
        String wildcardKeyword = "*" + keyword + "*";

        return NativeQuery.builder()
                .withQuery(q -> q.queryString(qs -> qs
                        .query(wildcardKeyword) // 와일드카드 쿼리 적용
                        .fields(List.of(fields)) // 검색할 필드들
                        .analyzeWildcard(true)   // 와일드카드 분석 활성화
                ))
                .withMaxResults(size)
                .build();
    }

    // 2. 페이지네이션용 쿼리 빌더
    private NativeQuery buildNativeQuery(String keyword, Pageable pageable, String... fields) {
        String wildcardKeyword = "*" + keyword + "*";

        return NativeQuery.builder()
                .withQuery(q -> q.queryString(qs -> qs
                        .query(wildcardKeyword)
                        .fields(List.of(fields))
                        .analyzeWildcard(true)
                ))
                .withPageable(pageable)
                .build();
    }

    private SectionResultDTO mapHitsToSection(SearchHits<?> hits, String key) {
        SectionResultDTO dto = new SectionResultDTO();
        dto.setTitle(getTabTitle(key));
        dto.setTotalCount((int) hits.getTotalHits());
        List<SearchResultItemDTO> items = hits.getSearchHits().stream()
                .map(hit -> convertDocumentToDTO(hit.getContent(), key))
                .collect(Collectors.toList());
        dto.setResults(items);
        return dto;
    }

    private SearchResultItemDTO convertDocumentToDTO(Object doc, String type) {
        SearchResultItemDTO item = new SearchResultItemDTO();
        try {
            switch (type) {
                case "product":
                    ProductDocument p = (ProductDocument) doc;
                    item.setTitle(p.getDpstName());
                    item.setSummary(safeSummary(p.getDpstInfo()));
                    item.setUrl("/deposit/view?dpstId=" + p.getDpstId());
                    break;
                case "faq":
                    FaqDocument f = (FaqDocument) doc;
                    item.setTitle(f.getFaqQuestion());
                    item.setSummary(safeSummary(f.getFaqAnswer()));
                    item.setUrl("/customer/faq_list");
                    break;
                case "docs":
                    TermDocument t = (TermDocument) doc;
                    item.setTitle(t.getTermTitle() + " (v" + t.getThistVersion() + ")");
                    item.setSummary(safeSummary(t.getThistContent()));
                    if (t.getThistFile() != null && !t.getThistFile().isBlank()) {
                        item.setUrl(t.getThistFile());
                    } else {
                        item.setUrl("/customer/terms/" + t.getThistNo());
                    }

                    if (t.getThistRegDy() != null) item.setExtra(t.getThistRegDy().format(DATE_FMT));
                    break;
                case "notice":
                    NoticeDocument n = (NoticeDocument) doc;
                    item.setTitle("[공지] " + n.getBoardTitle());
                    item.setSummary(safeSummary(n.getBoardContent()));
                    item.setUrl("/customer/notice_view/" + n.getBoardNo());
                    if (n.getBoardRegDt() != null) item.setExtra(n.getBoardRegDt().format(DATE_FMT));
                    break;
                case "event":
                    EventDocument e = (EventDocument) doc;
                    item.setTitle("[이벤트] " + e.getBoardTitle());
                    item.setSummary(safeSummary(e.getBoardContent()));
                    item.setUrl("/customer/event_view/" + e.getBoardNo());
                    if (e.getBoardRegDt() != null) item.setExtra(e.getBoardRegDt().format(DATE_FMT));
                    break;
            }
        } catch (Exception e) {
            log.warn("DTO 매핑 오류: {}", e.getMessage());
        }
        return item;
    }

    private String safeSummary(String content) {
        if (content == null) return "";
        return content.length() > 60 ? content.substring(0, 60) + "..." : content;
    }

    private String getTabTitle(String key) {
        return switch (key) {
            case "product" -> "상품";
            case "faq" -> "FAQ";
            case "docs" -> "약관";
            case "notice" -> "공지사항";
            case "event" -> "이벤트";
            default -> key;
        };
    }

    // 검색어 삭제 기능
    public void deleteSearchKeyword(String keyword, String custCode) {
        if (keyword == null || keyword.trim().isEmpty()) return;
        if (custCode == null) return;

        searchMapper.deleteSearchLog(keyword.trim(), custCode);
    }
}