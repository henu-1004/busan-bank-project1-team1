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

        String orgName = file.getOriginalFilename();
        String storedName = UUID.randomUUID() + "_" + orgName;
        String fullPath = uploadDir + "/" + storedName;

        File target = new File(fullPath);
        file.transferTo(target);

        // DTO 구성
        PdfAiDTO dto = new PdfAiDTO();
        dto.setOrgFileName(orgName);
        dto.setStoredFileName(storedName);
        dto.setFilePath(fullPath);
        dto.setStatus("wait");

        // INSERT
        pdfAiMapper.insertPdf(dto);

        // ⭐ INSERT 된 row의 PDF_ID 조회
        Long newId = pdfAiMapper.findInsertedId(dto);
        dto.setPdfId(newId);

        return newId;   // ✔ 이제 null 아님!
    }

}
