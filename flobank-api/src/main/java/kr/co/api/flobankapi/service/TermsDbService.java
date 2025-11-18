package kr.co.api.flobankapi.service;

import kr.co.api.flobankapi.dto.*;
import kr.co.api.flobankapi.mapper.TermsDbMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TermsDbService {

    private final TermsDbMapper mapper;
    private final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 전체 약관 목록 조회 */
    public List<TermsMasterDTO> getAllTerms() {
        return mapper.selectAllTermsWithLatestVersion();
    }

    /** 특정 약관 최신 버전 조회 */
    public TermsHistDTO getLatestHist(int cate, int order) {
        return mapper.selectLatestHist(cate, order);
    }

    /** 약관 신규 등록 (MASTER + HIST v1) */
    @Transactional
    public void createTerms(int cate, String title, String content, String adminId) {

        String today = LocalDate.now().format(FMT);

        // MASTER INSERT
        TermsMasterDTO master = new TermsMasterDTO();
        master.setTermCate(cate);
        master.setTermTitle(title);
        master.setTermRegDy(today);
        mapper.insertTermsMaster(master);

        // 신규 생성된 order 조회
        Integer order = mapper.selectMaxOrderByCate(cate);

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

        // MASTER 수정
        TermsMasterDTO master = new TermsMasterDTO();
        master.setTermCate(cate);
        master.setTermOrder(order);
        master.setTermTitle(title);
        mapper.updateTermsMaster(master);

        // HIST 새 버전 생성
        TermsHistDTO hist = new TermsHistDTO();
        hist.setThistTermCate(cate);
        hist.setThistTermOrder(order);
        hist.setThistContent(content);
        hist.setThistVersion(currentVersion + 1);
        hist.setThistVerMemo("내용 수정");
        hist.setThistAdminId(adminId);
        hist.setThistRegDy(LocalDate.now().format(FMT));

        mapper.insertTermsHist(hist);
    }

    /** 삭제 (MASTER + HIST 전체 삭제) */
    @Transactional
    public void deleteTerms(int cate, int order) {
        mapper.deleteTermsHist(cate, order);
        mapper.deleteTerms(cate, order);
    }

    /** 고객 약관 동의 기록 */
    public void saveAgree(String custCode, int cate, int order) {
        TermsAgreeDTO dto = new TermsAgreeDTO();
        dto.setAgreeCustCode(custCode);
        dto.setAgreeTermCate(cate);
        dto.setAgreeTermOrder(order);
        mapper.saveAgreeHist(dto);
    }
}
