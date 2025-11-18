package kr.co.api.flobankapi.service;

import kr.co.api.flobankapi.dto.*;
import kr.co.api.flobankapi.mapper.TermsDbMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TermsDbService {

    private final TermsDbMapper mapper;
    private final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 전체 약관 목록 조회 */
    public List<TermsMasterDTO> getAllTerms() {
        log.info("[약관 전체 조회] 시작");
        List<TermsMasterDTO> list = mapper.selectAllTermsWithLatestVersion();
        log.info("[약관 전체 조회] 총 {}건", list.size());
        return list;
    }

    /** 특정 약관 최신 버전 조회 */
    public TermsHistDTO getLatestHist(int cate, int order) {
        log.info("[최신 약관 조회] cate={}, order={}", cate, order);
        TermsHistDTO dto = mapper.selectLatestHist(cate, order);

        if (dto == null) {
            log.warn("[최신 약관 조회] 결과 없음");
        } else {
            log.info("[최신 약관 조회] version={}, regDy={}",
                    dto.getThistVersion(), dto.getThistRegDy());
        }

        return dto;
    }

    /** 약관 신규 등록 (MASTER + HIST v1) */
    @Transactional
    public void createTerms(int cate, String title, String content, String adminId) {

        String today = LocalDate.now().format(FMT);

        // ★ term_order 직접 생성
        Integer order = mapper.selectMaxOrderByCate(cate);
        order = (order == null) ? 1 : order + 1;

        // MASTER INSERT
        TermsMasterDTO master = new TermsMasterDTO();
        master.setTermCate(cate);
        master.setTermOrder(order);
        master.setTermTitle(title);
        master.setTermRegDy(today);
        mapper.insertTermsMaster(master);

        // HIST INSERT
        TermsHistDTO hist = new TermsHistDTO();
        hist.setThistTermCate(cate);
        hist.setThistTermOrder(order);
        hist.setThistContent(content);
        hist.setThistVersion(1);
        hist.setThistVerMemo("초기 등록");
        hist.setThistAdminId(adminId);
        hist.setThistRegDy(today);

        mapper.insertTermsHist(hist);
    }


    /** 수정 → HIST 새 버전 생성 */
    @Transactional
    public void updateTerms(int cate, int order, String title,
                            String content, int currentVersion, String adminId) {

        log.info("=== [약관 수정 시작] ===");
        log.info("카테고리={}, 순번={}, 현재버전={}, 새로운제목={}",
                cate, order, currentVersion, title);

        // MASTER 수정
        TermsMasterDTO master = new TermsMasterDTO();
        master.setTermCate(cate);
        master.setTermOrder(order);
        master.setTermTitle(title);
        mapper.updateTermsMaster(master);
        log.info("[MASTER UPDATE 완료]");

        // HIST 새 버전 추가
        TermsHistDTO hist = new TermsHistDTO();
        hist.setThistTermCate(cate);
        hist.setThistTermOrder(order);
        hist.setThistContent(content);
        hist.setThistVersion(currentVersion + 1);
        hist.setThistVerMemo("내용 수정");
        hist.setThistAdminId(adminId);
        hist.setThistRegDy(LocalDate.now().format(FMT));

        mapper.insertTermsHist(hist);

        log.info("[HIST NEW VERSION INSERT 완료] → 신규버전={}", currentVersion + 1);
        log.info("=== [약관 수정 종료] ===");
    }

    /** 삭제 (MASTER + HIST 전체 삭제) */
    @Transactional
    public void deleteTerms(int cate, int order) {

        log.warn("=== [약관 삭제 시작] cate={}, order={} ===", cate, order);

        mapper.deleteTermsHist(cate, order);
        log.warn("[HIST 삭제 완료]");

        mapper.deleteTerms(cate, order);
        log.warn("[MASTER 삭제 완료]");

        log.warn("=== [약관 삭제 종료] ===");
    }

    /** 고객 약관 동의 기록 */
    public void saveAgree(String custCode, int cate, int order) {
        log.info("[약관 동의 기록] cust={}, cate={}, order={}", custCode, cate, order);

        TermsAgreeDTO dto = new TermsAgreeDTO();
        dto.setAgreeCustCode(custCode);
        dto.setAgreeTermCate(cate);
        dto.setAgreeTermOrder(order);

        mapper.saveAgreeHist(dto);

        log.info("[약관 동의 기록 완료]");
    }


    public Map<String, Object> getTermsPage(int page, int pageSize) {

        int start = (page - 1) * pageSize;

        List<TermsMasterDTO> list = mapper.selectTermsPage(start, pageSize);
        int totalCount = mapper.countTerms();

        int totalPage = (int) Math.ceil(totalCount / (double) pageSize);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("totalPage", totalPage);

        return result;
    }

    public Map<String, Object> getTermsPage(int page, int pageSize,
                                            String type, String keyword) {

        int start = (page - 1) * pageSize;

        List<TermsMasterDTO> list =
                mapper.selectTermsPageFiltered(start, pageSize, type, keyword);

        int totalCount =
                mapper.countTermsFiltered(type, keyword);

        int totalPage = (int) Math.ceil(totalCount / (double) pageSize);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("totalPage", totalPage);

        return result;
    }


    public Map<String, Object> getTermsDetail(int cate, int order) {

        TermsMasterDTO master = mapper.selectMaster(cate, order);
        TermsHistDTO latest = mapper.selectLatestHist(cate, order);

        Map<String, Object> result = new HashMap<>();
        result.put("title", master.getTermTitle());
        result.put("version", latest.getThistVersion());
        result.put("regDy", latest.getThistRegDy());
        result.put("adminId", latest.getThistAdminId());
        result.put("content", latest.getThistContent());

        return result;
    }



}
