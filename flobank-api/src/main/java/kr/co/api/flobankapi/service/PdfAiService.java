package kr.co.api.flobankapi.service;

import kr.co.api.flobankapi.dto.PdfAiDTO;
import kr.co.api.flobankapi.mapper.PdfAiMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;   // ✔ THIS
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;    // ✔ THIS

import java.io.File;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfAiService {

    @Value("${file.upload.pdf-ai-path}")
    private String uploadDir;

    private final PdfAiMapper pdfAiMapper;

    public List<PdfAiDTO> getAllPdfs() {
        return pdfAiMapper.findAll();
    }


    public Long savePdf(MultipartFile file) throws Exception {

        // 원본 파일명
        String orgName = file.getOriginalFilename();

        // 저장 파일명 (UUID 붙이기)
        String storedName = UUID.randomUUID() + "_" + orgName;

        // 실제 파일 저장 경로
        String fullPath = uploadDir + "/" + storedName;

        // 서버에 파일 저장
        File target = new File(fullPath);
        file.transferTo(target);

        // DB insert
        PdfAiDTO dto = new PdfAiDTO();
        dto.setOrgFileName(orgName);
        dto.setStoredFileName(storedName);
        dto.setFilePath(fullPath);
        dto.setStatus("wait");  // 초기 상태

        pdfAiMapper.insertPdf(dto);

        return dto.getPdfId();
    }
}
