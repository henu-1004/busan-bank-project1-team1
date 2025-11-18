package kr.co.api.flobankapi.service;

import kr.co.api.flobankapi.dto.*;
import kr.co.api.flobankapi.mapper.TermsDbMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

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

        log.info("=== [약관 등록 시작] ===");
        log.info("카테고리: {}, 제목: {}", cate, title);

        // MASTER INSERT
        TermsMasterDTO master = new TermsMasterDTO();
        master.setTermCate(cate);
        master.setTermTitle(title);
        master.setTermRegDy(today);
        mapper.insertTermsMaster(master);

        log.info("[MASTER INSERT 완료]");

        // 생성된 term_order 조회
        Integer order = mapper.selectMaxOrderByCate(cate);
        log.info("[ORDER 생성 완료] → {}", order);

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

        log.info("[HIST INSERT 완료]");
        log.info("=== [약관 등록 종료] ===");
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
}
