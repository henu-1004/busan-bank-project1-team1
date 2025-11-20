/*
 * 날짜 : 2025/11/20
 * 이름 : 김대현
 * 내용 : 디비 불러오기 수정
 * */

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

import java.io.File;
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

        log.info("📁 [DEBUG] pdfTermsPath = {}", filePathConfig.getPdfTermsPath());


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
        - DB에 혹시 기존 버전이 있다면(테스트 데이터 등),
          그 중 가장 최신 버전을 기준으로 +1 해서 새 버전 저장
    ============================================================ */

        TermsHistDTO latest = mapper.selectLatestHist(cate, order);
        int newVersion = (latest == null ? 1 : latest.getThistVersion() + 1);
        log.info("[HIST 버전 계산] cate={}, order={}, latestVer={}, newVer={}",
                cate, order,
                latest == null ? null : latest.getThistVersion(),
                newVersion);

    /* ============================================================
        HIST INSERT
    ============================================================ */
        TermsHistDTO hist = new TermsHistDTO();
        hist.setThistTermCate(cate);
        hist.setThistTermOrder(order);
        hist.setThistContent(content);
        hist.setThistVersion(newVersion);
        hist.setThistVerMemo("초기 등록");
        hist.setThistAdminId(adminId);
        hist.setThistRegDy(today);
        hist.setThistFile(savedFilePath);

        mapper.insertTermsHist(hist);

        log.info("[HIST 신규 등록 완료] cate={}, order={}, version={}, file={}",
                cate, order, newVersion, savedFilePath);
    }

    // PDF 저장

    private String saveTermsPdf(MultipartFile file) throws Exception {
        String basePath = filePathConfig.getPdfTermsPath(); // 예: /app/uploads/terms

        if (basePath == null || basePath.isBlank()) {
            log.warn("⚠ 파일 업로드 경로가 설정되지 않음 → 파일 저장 스킵");
            return null;
        }

        // 1. 원본 파일명 정리 (특수문자 제거 등)
        String original = file.getOriginalFilename();
        String safeName = StringUtils.cleanPath(original).replaceAll("[^a-zA-Z0-9._-]", "_");

        // 2. 유니크 파일명 생성
        String stored = UUID.randomUUID() + "_" + safeName;

        // 3. 저장할 파일 객체 생성
        // Paths.get(...).toFile() 대신 new File(...)을 사용하여 제어합니다.
        File dest = new File(basePath, stored);

        if (!dest.isAbsolute()) {
            dest = dest.getAbsoluteFile();
        }

        log.info(" 실제 저장 시도 경로: {}", dest.getPath());

        //  [핵심] 저장하려는 '그 파일'의 부모 폴더가 없으면 생성
        if (!dest.getParentFile().exists()) {
            dest.getParentFile().mkdirs();
        }

        // 4. 파일 저장
        file.transferTo(dest);

        // 5. 브라우저 접근용 URL 반환
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

    /* ============================================================
        🔥 DB 기준 최신 버전 조회 후 +1
        - currentVersion 파라미터는 이제 '참고용'이 되고,
          실제 저장되는 버전은 DB에 있는 최신 값 기반으로 계산
    ============================================================ */
        TermsHistDTO latest = mapper.selectLatestHist(cate, order);
        int newVersion = (latest == null ? 1 : latest.getThistVersion() + 1);
        log.info("[HIST 수정 버전 계산] cate={}, order={}, dbLatestVer={}, newVer={}",
                cate, order,
                latest == null ? null : latest.getThistVersion(),
                newVersion);

        // HIST 새 버전 INSERT
        TermsHistDTO hist = new TermsHistDTO();
        hist.setThistTermCate(cate);
        hist.setThistTermOrder(order);
        hist.setThistContent(content);
        hist.setThistVersion(newVersion);
        hist.setThistVerMemo(verMemo != null && !verMemo.isEmpty() ? verMemo : "내용 수정");
        hist.setThistAdminId(adminId);
        hist.setThistRegDy(LocalDate.now().format(FMT));

        mapper.insertTermsHist(hist);

        log.info("[HIST NEW VERSION INSERT 완료] → 신규버전={}", newVersion);
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


    // import kr.co.api.flobankapi.dto.TermsHistDTO; 이미 위에 있으니 그대로 둬도 됨
    public List<TermsHistDTO> getTermsByLocation(int termCate) {

        // Mapper에서 이미 "카테고리별 최신버전 1개"만 가져오도록 쿼리가 짜져 있으므로
        // 추가로 중복 제거할 필요 없이 그대로 반환
        return mapper.selectTermsByCate(termCate);
    }





}
