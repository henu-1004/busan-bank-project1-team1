package kr.co.api.flobankapi.controller;

import kr.co.api.flobankapi.service.PdfAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/admin/pdf-ai")
@RequiredArgsConstructor
public class PdfAiController {

    private final PdfAiService pdfAiService;

    // PDF 업로드
    @PostMapping("/upload")
    public ResponseEntity<?> uploadPdf(@RequestParam("file") MultipartFile file) {
        try {
            Long pdfId = pdfAiService.savePdf(file);
            return ResponseEntity.ok(pdfId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("업로드 실패: " + e.getMessage());
        }
    }

    // PDF 목록 조회
    @GetMapping("/list")
    public ResponseEntity<?> getPdfList() {
        return ResponseEntity.ok(pdfAiService.getAllPdfs());
    }
}
