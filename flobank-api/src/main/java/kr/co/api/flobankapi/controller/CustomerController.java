package kr.co.api.flobankapi.controller;

import kr.co.api.flobankapi.dto.BoardDTO;
import kr.co.api.flobankapi.dto.FaqDTO;
import kr.co.api.flobankapi.dto.QnaDTO;
import kr.co.api.flobankapi.dto.TermsHistDTO;
import kr.co.api.flobankapi.jwt.CustomUserDetails;
import kr.co.api.flobankapi.service.BoardService;
import kr.co.api.flobankapi.service.FaqService;
import kr.co.api.flobankapi.service.QnaService;
import kr.co.api.flobankapi.service.TermsDbService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final BoardService boardService;
    private final FaqService faqService;
    private final TermsDbService termsDbService;
    private final QnaService qnaService;

    @GetMapping("/event_list")
    public String event_list(
            @RequestParam(defaultValue = "1") int page,
            Model model) {

        Map<String, Object> eventPage = boardService.getEventPage(page);

        model.addAttribute("eventList", eventPage.get("list"));
        model.addAttribute("page", eventPage.get("page"));
        model.addAttribute("totalPage", eventPage.get("totalPage"));
        model.addAttribute("totalCount", eventPage.get("totalCount"));
        model.addAttribute("pageSize", eventPage.get("pageSize"));
        model.addAttribute("activeItem","event");

        return "customer/event_list";
    }

    @GetMapping("/event_view/{boardNo}")
    public String event_view(@PathVariable Long boardNo, Model model){

        BoardDTO event = boardService.getEvent(boardNo);

        if (event == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        model.addAttribute("activeItem", "event");
        model.addAttribute("event", event);

        return "customer/event_view";
    }

    @GetMapping("/faq_list")
    public String faqList(Model model) {

        List<FaqDTO> faqList = faqService.getFaqList();

        Map<Integer, List<FaqDTO>> grouped = faqList.stream()
                .collect(Collectors.groupingBy(FaqDTO::getFaqCate));

        model.addAttribute("faqMap", grouped);
        model.addAttribute("activeItem", "faq");

        return "customer/faq_list";
    }

    @GetMapping("/intro")
    public String intro(Model model){
        model.addAttribute("activeItem","intro");
        return "customer/intro";
    }

    @GetMapping("/notice_list")
    public String notice_list(
            @RequestParam(defaultValue = "1") int page,
            Model model){

        Map<String, Object> noticePage = boardService.getNoticePage(page);

        model.addAttribute("noticeList", noticePage.get("list"));
        model.addAttribute("page", noticePage.get("page"));
        model.addAttribute("totalPage", noticePage.get("totalPage"));
        model.addAttribute("totalCount", noticePage.get("totalCount"));
        model.addAttribute("pageSize", noticePage.get("pageSize"));
        model.addAttribute("activeItem","notice");

        return "customer/notice_list";
    }

    @GetMapping("/notice_view/{boardNo}")
    public String notice_view(@PathVariable Long boardNo, Model model){
        BoardDTO notice = boardService.getNotice(boardNo);
        if (notice == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        model.addAttribute("activeItem", "notice");
        model.addAttribute("notice", notice);
        return "customer/notice_view";
    }

    @GetMapping("/terms_download")
    public String termsDownload(Model model) {
        int[] categories = {1, 2, 3, 5, 6};

        Map<Integer, List<TermsHistDTO>> termsByCate = new LinkedHashMap<>();
        Map<Integer, String> categoryNames = new LinkedHashMap<>();

        categoryNames.put(1, "회원가입");
        categoryNames.put(2, "환전하기");
        categoryNames.put(3, "외화송금");
        categoryNames.put(5, "원화통장 개설");
        categoryNames.put(6, "외화통장 개설");

        for (int cate : categories) {
            List<TermsHistDTO> terms = termsDbService.getTermsByLocation(cate);
            termsByCate.put(cate, terms == null ? Collections.emptyList() : terms);
        }

        model.addAttribute("categoryNames", categoryNames);
        model.addAttribute("termsByCate", termsByCate);
        model.addAttribute("activeItem", "terms");

        return "customer/terms_download";
    }

    @GetMapping("/terms_download/{histId}/file")
    public ResponseEntity<Resource> downloadTermsFile(@PathVariable Long histId) {
        TermsHistDTO hist = termsDbService.getTermsHist(histId);

        if (hist == null || hist.getThistFile() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        Resource resource = termsDbService.loadTermsFile(hist);

        if (resource == null || !resource.exists() || !resource.isReadable()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        String downloadName = termsDbService.buildDownloadFileName(hist);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + downloadName + "\"")
                .body(resource);
    }

    @GetMapping("/qna_edit")
    public String qna_edit(Model model){
        model.addAttribute("activeItem","qna");
        return "customer/qna_edit";
    }

    @GetMapping("/qna_list")
    public String qna_list(@RequestParam(defaultValue = "1") int page, Model model){

        Map<String, Object> qnaPage = qnaService.getQnaPage(page);

        model.addAttribute("qnaList", qnaPage.get("list"));
        model.addAttribute("page", qnaPage.get("page"));
        model.addAttribute("totalPage", qnaPage.get("totalPage"));
        model.addAttribute("totalCount", qnaPage.get("totalCount"));
        model.addAttribute("pageSize", qnaPage.get("pageSize"));
        model.addAttribute("activeItem","qna");

        return "customer/qna_list";
    }

    @GetMapping("/qna_view/{qnaNo}")
    public String qna_view(@PathVariable Long qnaNo,
                           Model model,
                           @AuthenticationPrincipal CustomUserDetails userDetails){

        QnaDTO qna = qnaService.getQna(qnaNo);
        if (qna == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        boolean canModify = userDetails != null && userDetails.getUsername().equals(qna.getQnaCustCode());

        model.addAttribute("activeItem","qna");
        model.addAttribute("qna", qna);
        model.addAttribute("canModify", canModify);

        return "customer/qna_view";
    }

    @GetMapping("/qna_write")
    public String qna_write(Model model, @AuthenticationPrincipal CustomUserDetails userDetails){
        model.addAttribute("activeItem","qna");

        if (userDetails != null) {
            model.addAttribute("customerId", userDetails.getUsername());
            model.addAttribute("customerName", userDetails.getCustName());
        }

        return "customer/qna_write";
    }

    @PostMapping("/qna_write")
    public String submitQna(QnaDTO qnaDTO,
                            @AuthenticationPrincipal CustomUserDetails userDetails,
                            RedirectAttributes redirectAttributes){

        if (userDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        qnaDTO.setQnaCustCode(userDetails.getUsername());
        qnaService.insertQna(qnaDTO);

        redirectAttributes.addFlashAttribute("message", "문의가 등록되었습니다.");

        return "redirect:/customer/qna_list";
    }
}
