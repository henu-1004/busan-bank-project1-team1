package kr.co.api.flobankapi.service;

import kr.co.api.flobankapi.dto.PdfAiDTO;
import kr.co.api.flobankapi.mapper.PdfAiMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfAiService {

    @Value("${file.upload.pdf-ai-path}")
    private String uploadDir;

    @Value("${ai-server.url}")
    private String aiServerUrl;

    private final PdfAiMapper pdfAiMapper;
    private final WebClient webClient;


    public List<PdfAiDTO> getAllPdfs() {
        return pdfAiMapper.findAll();
    }

    // 파일 저장 + DB 저장 + AI 서버 Webhook 전송
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

        // ⭐ AI 서버에서 실제로 다운로드할 수 있는 URL 생성
        String downloadUrl = "http://34.64.124.33:8080/pdf_ai/" + storedName;
        dto.setDownloadUrl(downloadUrl);

        // DB INSERT
        pdfAiMapper.insertPdf(dto);

        // INSERT된 PDF_ID 가져오기
        Long newId = pdfAiMapper.findInsertedId(dto);
        dto.setPdfId(newId);

        // ⭐ 전체 DTO를 AI 서버로 비동기 전송
        sendWebhookAsync(dto);

        return newId;
    }

    // 🔥 Webhook 비동기 호출 (DTO 전체 전달)
    @Async
    public void sendWebhookAsync(PdfAiDTO dto) {

        String url = aiServerUrl + "/api/pdf/process";

        webClient.post()
                .uri(url)
                .bodyValue(dto)   // 👈 DTO 전체(JSON) 그대로 보냄
                .retrieve()
                .toBodilessEntity()
                .doOnSuccess(res ->
                        log.info("AI 서버에 Webhook 전송 성공 → pdfId={}", dto.getPdfId())
                )
                .doOnError(err ->
                        log.error("AI 서버 Webhook 실패: {}", err.getMessage())
                )
                .subscribe();
    }
}
