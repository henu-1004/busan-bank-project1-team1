package kr.co.api.flobankapi.mapper;

import kr.co.api.flobankapi.dto.PdfAiDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PdfAiMapper {

    // 1) PDF 등록 (insert)
    void insertPdf(PdfAiDTO dto);

    // 2) 상태별 PDF 목록 조회 (wait/done/used)
    List<PdfAiDTO> findByStatus(String status);

    // 3) 전체 PDF 목록 조회 (UI에 목록 띄울 때)
    List<PdfAiDTO> findAll();

    // 4) ID로 조회 (AI 서버가 가져갈 때)
    PdfAiDTO findById(Long pdfId);

    // 5) 상태 업데이트 (wait → done / done → used)
    void updateStatus(Long pdfId, String status);
}
