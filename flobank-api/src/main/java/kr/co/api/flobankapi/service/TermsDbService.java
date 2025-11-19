package kr.co.api.flobankapi.service;

import kr.co.api.flobankapi.dto.*;
import kr.co.api.flobankapi.mapper.TermsDbMapper;
import kr.co.api.flobankapi.config.FilePathConfig; // ⭐ 추가
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;                // ⭐ 추가

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.nio.file.*;                               // ⭐ 추가

@Service
@RequiredArgsConstructor
@Slf4j
public class TermsDbService {

    private final TermsDbMapper mapper;
    private final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final FilePathConfig filePathConfig;      // ⭐ 추가됨


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


    /** =====================================================================================
     약관 신규 등록 (MASTER + HIST v1 + 파일 업로드)
     ===================================================================================== */
    @Transactional
    public void createTerms(int cate, String title, String content,
                            String adminId, MultipartFile file) throws Exception {

        String today = LocalDate.now().format(FMT);

        // ★ term_order 생성
        Integer order = mapper.selectMaxOrderByCate(cate);
        order = (order == null) ? 1 : order + 1;

        // MASTER INSERT
        TermsMasterDTO master = new TermsMasterDTO();
        master.setTermCate(cate);
        master.setTermOrder(order);
        master.setTermTitle(title);
        master.setTermRegDy(today);
        mapper.insertTermsMaster(master);

        /* ============================================================
            ⭐ 파일 업로드 처리
        ============================================================ */
        String savedFilePath = null;

        if (file != null && !file.isEmpty()) {
            savedFilePath = saveTermsPdf(file);    // ⭐ 파일 저장 실행
            log.info("[PDF 저장 완료] {}", savedFilePath);
        }

        /* ============================================================
            HIST INSERT
        ============================================================ */
        TermsHistDTO hist = new TermsHistDTO();
        hist.setThistTermCate(cate);
        hist.setThistTermOrder(order);
        hist.setThistContent(content);
        hist.setThistVersion(1);
        hist.setThistVerMemo("초기 등록");
        hist.setThistAdminId(adminId);
        hist.setThistRegDy(today);

        hist.setThistFile(savedFilePath);         // ⭐ 저장된 파일 경로 HIST에 기록

        mapper.insertTermsHist(hist);

        log.info("[HIST v1 등록 완료] file={}", savedFilePath);
    }



    /** =====================================================================================
     파일 저장 함수
     ===================================================================================== */
    private String saveTermsPdf(MultipartFile file) throws Exception {

        String basePath = filePathConfig.getPdfTermsPath(); // yml에서 가져옴

        // 폴더 없으면 생성
        Files.createDirectories(Paths.get(basePath));

        // 원본 파일명 처리
        String original = file.getOriginalFilename();
        String safeName = StringUtils.cleanPath(original);
        safeName = safeName.replaceAll("[^a-zA-Z0-9._-]", "_");

        // 유니크 파일명 생성
        String stored = UUID.randomUUID() + "_" + safeName;

        // 실제 저장 위치
        Path path = Paths.get(basePath, stored);
        file.transferTo(path.toFile());

        // 브라우저 접근용 URL 반환
        return "/uploads/terms/" + stored;
    }



    /** =====================================================================================
     약관 수정 → HIST 새 버전 생성
     ===================================================================================== */
    @Transactional
    public void updateTerms(int cate, int order, String title,
                            String content, int currentVersion, String adminId, String verMemo) {

        log.info("=== [약관 수정 시작] ===");
        log.info("cate={}, order={}, currentVersion={}, newTitle={}",
                cate, order, currentVersion, title);

        // MASTER title 수정
        TermsMasterDTO master = new TermsMasterDTO();
        master.setTermCate(cate);
        master.setTermOrder(order);
        master.setTermTitle(title);
        mapper.updateTermsMaster(master);

        // HIST 새 버전 INSERT
        TermsHistDTO hist = new TermsHistDTO();
        hist.setThistTermCate(cate);
        hist.setThistTermOrder(order);
        hist.setThistContent(content);
        hist.setThistVersion(currentVersion + 1);
        hist.setThistVerMemo(verMemo != null && !verMemo.isEmpty() ? verMemo : "내용 수정");
        hist.setThistAdminId(adminId);
        hist.setThistRegDy(LocalDate.now().format(FMT));

        mapper.insertTermsHist(hist);

        log.info("[HIST NEW VERSION INSERT 완료] → 신규버전={}", currentVersion + 1);
    }



    /** 고객 약관 동의 기록 */
    public void saveAgree(String custCode, int cate, int order) {
        TermsAgreeDTO dto = new TermsAgreeDTO();
        dto.setAgreeCustCode(custCode);
        dto.setAgreeTermCate(cate);
        dto.setAgreeTermOrder(order);

        mapper.saveAgreeHist(dto);
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

        Map<String, Object> result = new HashMap<>();

        TermsMasterDTO master = mapper.selectMaster(cate, order);
        TermsHistDTO latest = mapper.selectLatestHist(cate, order);

        if (master == null) {
            result.put("title", "제목 없음");
            result.put("version", 0);
            result.put("regDy", "-");
            result.put("adminId", "-");
            result.put("content", "");
            result.put("verMemo", "-");
            return result;
        }

        result.put("title", master.getTermTitle());

        if (latest == null) {
            result.put("version", 1);
            result.put("regDy", master.getTermRegDy());
            result.put("adminId", "관리자");
            result.put("content", "");
            result.put("verMemo", "-");
        } else {
            result.put("version", latest.getThistVersion());
            result.put("regDy", latest.getThistRegDy());
            result.put("adminId", latest.getThistAdminId());
            result.put("content", latest.getThistContent());
            result.put("verMemo", latest.getThistVerMemo());
            result.put("file", latest.getThistFile());     // ⭐ 파일도 포함
        }

        return result;
    }



    public List<TermsHistDTO> getTermsByLocation(int termCate) {
        return mapper.selectTermsByCate(termCate);
    }

}
