package kr.co.api.flobankapi.controller.admin;

import jakarta.servlet.http.HttpServletResponse;
import kr.co.api.flobankapi.dto.*;
import kr.co.api.flobankapi.dto.admin.dashboard.DashboardDTO;
import kr.co.api.flobankapi.service.*;
import kr.co.api.flobankapi.service.admin.AdminAuthService;
import kr.co.api.flobankapi.service.admin.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final BoardService boardService;
    private final FaqService faqService;
    private final QnaService qnaService;
    private final AdminAuthService adminAuthService;
    private final DashboardService dashboardService;


    /** 목록 */
    @GetMapping("/member")
    public String member(@RequestParam(defaultValue = "1") int page,
                         @RequestParam(name = "faqPage", defaultValue = "1") int faqPage,
                         @RequestParam(name = "qnaPage", defaultValue = "1") int qnaPage,
                         @RequestParam(name = "qnaStatus", defaultValue = "all") String qnaStatus,
                         Model model,
                         @ModelAttribute("msg") String msg) {

        // 1) 공지/이벤트 페이징
        Map<String, Object> boardPage = boardService.getAllBoardPage(page);

        model.addAttribute("list", boardPage.get("list"));
        model.addAttribute("page", boardPage.get("page"));
        model.addAttribute("pageSize", boardPage.get("pageSize"));
        model.addAttribute("totalPage", boardPage.get("totalPage"));
        model.addAttribute("totalCount", boardPage.get("totalCount"));
        model.addAttribute("editMode", false);   // 게시판 등록 모드 기본

        // 2) FAQ 페이징
        Map<String, Object> faqPageData = faqService.getFaqPage(faqPage);

        model.addAttribute("faqList", faqPageData.get("faqList"));
        model.addAttribute("faqPage", faqPageData.get("faqPage"));
        model.addAttribute("faqPageSize", faqPageData.get("faqPageSize"));
        model.addAttribute("totalFaqPage", faqPageData.get("totalFaqPage"));
        model.addAttribute("totalFaqCount", faqPageData.get("totalFaqCount"));
        model.addAttribute("faqEditMode", false); // FAQ 등록 모드 기본

        // 3) QNA 페이징
        Map<String, Object> qnaPageData = qnaService.getAdminQnaPage(qnaPage, qnaStatus);

        model.addAttribute("qnaList", qnaPageData.get("list"));
        model.addAttribute("qnaPage", qnaPageData.get("page"));
        model.addAttribute("qnaPageSize", qnaPageData.get("pageSize"));
        model.addAttribute("totalQnaPage", qnaPageData.get("totalPage"));
        model.addAttribute("totalQnaCount", qnaPageData.get("totalCount"));
        model.addAttribute("qnaStatus", qnaPageData.get("status"));

        model.addAttribute("activeItem", "member");

        return "admin/member";
    }





    /** 외화예금 상세 보기 */
    @GetMapping("/products_view")
    public String productsView(Model model) {
        model.addAttribute("activeItem", "products");
        return "admin/products_view";
    }


    /** 글 등록 */
    @PostMapping("/board/register")
    public String registerBoard(BoardDTO board, RedirectAttributes ra) {
        boardService.insertBoard(board);
        ra.addFlashAttribute("msg", "등록이 완료되었습니다!");
        return "redirect:/admin/member";
    }

    /** 글 삭제 */
    @GetMapping("/board/delete/{boardNo}")
    public String deleteBoard(@PathVariable Long boardNo, RedirectAttributes ra) {
        boardService.deleteBoard(boardNo);
        ra.addFlashAttribute("msg", "삭제되었습니다!");
        return "redirect:/admin/member";
    }

    /** 글 편집 화면 열기 */
    @GetMapping("/board/edit/{boardNo}")
    public String editBoard(@PathVariable Long boardNo,
                            @RequestParam(defaultValue = "1") int page,
                            @RequestParam(name = "faqPage", defaultValue = "1") int faqPage,
                            Model model) {

        // 1) 수정할 게시글
        BoardDTO board = boardService.getBoardByNo(boardNo);

        // 2) 공지/이벤트 페이징
        Map<String, Object> boardPage = boardService.getAllBoardPage(page);
        model.addAttribute("list", boardPage.get("list"));
        model.addAttribute("page", boardPage.get("page"));
        model.addAttribute("pageSize", boardPage.get("pageSize"));
        model.addAttribute("totalPage", boardPage.get("totalPage"));
        model.addAttribute("totalCount", boardPage.get("totalCount"));
        model.addAttribute("editMode", true); // 수정 모드
        model.addAttribute("board", board);

        // 3) FAQ 페이징 (★ 반드시 등록 화면과 동일하게 유지)
        Map<String, Object> faqPageData = faqService.getFaqPage(faqPage);
        model.addAttribute("faqList", faqPageData.get("faqList"));
        model.addAttribute("faqPage", faqPageData.get("faqPage"));
        model.addAttribute("faqPageSize", faqPageData.get("faqPageSize"));
        model.addAttribute("totalFaqPage", faqPageData.get("totalFaqPage"));
        model.addAttribute("totalFaqCount", faqPageData.get("totalFaqCount"));
        model.addAttribute("faqEditMode", false);

        model.addAttribute("activeItem", "member");

        return "admin/member";
    }


    /** 글 수정 */
    @PostMapping("/board/update")
    public String updateBoard(BoardDTO board,
                              @RequestParam(defaultValue = "1") int page,
                              RedirectAttributes ra) {

        boardService.updateBoard(board);
        ra.addFlashAttribute("msg", "수정이 완료되었습니다!");

        return "redirect:/admin/member?page=" + page + "#board-list";
    }



    /** FAQ 등록 */
    @PostMapping("/faq/register")
    public String registerFaq(FaqDTO faq, RedirectAttributes ra) {

        faqService.insertFaq(faq);

        ra.addFlashAttribute("msg", "FAQ가 등록되었습니다!");

        // 다시 목록 화면으로
        return "redirect:/admin/member#faq-list";
    }


    /** FAQ 삭제 */
    @GetMapping("/faq/delete/{faqNo}")
    public String deleteFaq(@PathVariable Long faqNo,
                            RedirectAttributes ra) {

        faqService.deleteFaq(faqNo);
        ra.addFlashAttribute("msg", "FAQ가 삭제되었습니다!");

        return "redirect:/admin/member#faq-list";
    }

    @GetMapping("/faq/edit/{faqNo}")
    public String editFaq(@PathVariable Long faqNo,
                          @RequestParam(defaultValue = "1") int page,
                          @RequestParam(name="faqPage", defaultValue="1") int faqPage,
                          Model model,
                          @ModelAttribute("msg") String msg) {

        // 공지/이벤트 페이징
        Map<String, Object> boardPage = boardService.getAllBoardPage(page);
        model.addAttribute("list", boardPage.get("list"));
        model.addAttribute("page", boardPage.get("page"));
        model.addAttribute("pageSize", boardPage.get("pageSize"));
        model.addAttribute("totalPage", boardPage.get("totalPage"));
        model.addAttribute("totalCount", boardPage.get("totalCount"));
        model.addAttribute("editMode", false);

        // FAQ 페이징 유지
        Map<String, Object> faqPageData = faqService.getFaqPage(faqPage);
        model.addAttribute("faqList", faqPageData.get("faqList"));
        model.addAttribute("faqPage", faqPageData.get("faqPage"));
        model.addAttribute("faqPageSize", faqPageData.get("faqPageSize"));
        model.addAttribute("totalFaqPage", faqPageData.get("totalFaqPage"));
        model.addAttribute("totalFaqCount", faqPageData.get("totalFaqCount"));

        FaqDTO faq = faqService.getFaq(faqNo);
        model.addAttribute("faq", faq);

        model.addAttribute("faqEditMode", true);
        model.addAttribute("activeItem", "member");


        return "admin/member";
    }


    /** FAQ 수정 */
    @PostMapping("/faq/update")
    public String updateFaq(FaqDTO faq,
                            @RequestParam(defaultValue = "1") int faqPage,
                            RedirectAttributes ra) {

        faqService.updateFaq(faq);

        ra.addFlashAttribute("msg", "FAQ가 수정되었습니다!");

        return "redirect:/admin/member?faqPage=" + faqPage + "#faq-list";
    }

    @GetMapping("/qna/{qnaNo}")
    public String viewQna(@PathVariable Long qnaNo,
                          @RequestParam(name = "qnaPage", defaultValue = "1") int qnaPage,
                          @RequestParam(name = "qnaStatus", defaultValue = "all") String qnaStatus,
                          Model model,
                          @ModelAttribute("msg") String msg) {

        QnaDTO qna = qnaService.findQna(qnaNo);
        if (qna == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        model.addAttribute("qna", qna);
        model.addAttribute("qnaPage", qnaPage);
        model.addAttribute("qnaStatus", normalizeStatus(qnaStatus));
        model.addAttribute("msg", msg);
        model.addAttribute("activeItem", "member");

        return "admin/qna_view";
    }

    @PostMapping("/qna/{qnaNo}/reply")
    public String updateQnaReply(@PathVariable Long qnaNo,
                                 @RequestParam(name = "reply", required = false) String reply,
                                 @RequestParam(name = "qnaPage", defaultValue = "1") int qnaPage,
                                 @RequestParam(name = "qnaStatus", defaultValue = "all") String qnaStatus,
                                 RedirectAttributes redirectAttributes) {

        QnaDTO qna = qnaService.findQna(qnaNo);
        if (qna == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        qnaService.updateQnaReply(qnaNo, reply);
        redirectAttributes.addFlashAttribute("msg", "답변이 저장되었습니다.");

        return "redirect:/admin/qna/" + qnaNo + "?qnaPage=" + qnaPage + "&qnaStatus=" + normalizeStatus(qnaStatus);
    }



    @GetMapping("/login")
    public String adminLoginForm() {
        return "admin/login";
    }

    @PostMapping("/login")
    public String adminLoginProcess(@RequestParam("adminId") String adminId,
                                    @RequestParam("adminPw") String adminPw,
                                    @RequestParam("adminPh") String adminPh,
                                    @RequestParam("code") String code,
                                    HttpServletResponse response,
                                    RedirectAttributes redirectAttributes) {

        try {
            // ★ 여기서 TB_ADMIN_INFO 조회 + 비번/휴대폰/인증번호 검증 + JWT 발급 + 쿠키 세팅
            adminAuthService.login(adminId, adminPw, adminPh, code, response);
            log.info("▶ [ADMIN LOGIN] SUCCESS - {}", adminId);
            // 로그인 성공 → 관리자 대시보드로
            return "redirect:/admin";

        } catch (BadCredentialsException | IllegalArgumentException e) {
            log.info("▶ [ADMIN LOGIN] FAIL - id={}, reason={}", adminId, e.getMessage());
            // 로그인 실패 → 에러 메시지와 함께 다시 로그인 화면으로
            redirectAttributes.addFlashAttribute("loginError", e.getMessage());
            redirectAttributes.addFlashAttribute("adminId", adminId);
            redirectAttributes.addFlashAttribute("adminPh", adminPh);
            return "redirect:/admin/login";
        }

    }

    private final ChatbotRuleService chatbotRuleService;
    private final ChatbotSessionService chatbotSessionService;
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @GetMapping("/chatbot")
    public String adminChatbotForm(Model model) {
        ChatbotSessionDTO sessDTO = new ChatbotSessionDTO();
        sessDTO.setSessCustCode("admin");
        sessDTO.setSessStartDt(LocalDateTime.now().format(formatter));

        sessDTO = chatbotSessionService.insertSess(sessDTO);
        model.addAttribute("sessId", sessDTO.getSessId());


        if (!model.containsAttribute("faHistDTO")) { // 처음 진입 여부 확인
            ChatbotHistDTO emptyDTO = new ChatbotHistDTO();
            emptyDTO.setBotContent(""); // 빈값
            model.addAttribute("faHistDTO", emptyDTO);
        }


        List<ChatbotBadTypeDTO> badType = chatbotRuleService.selectBadTypeList();
        List<ChatbotBadWordDTO> badWord = chatbotRuleService.selectBadWordList();
        List<ChatbotRulesDTO> botRules = chatbotRuleService.selectRulesList();

        model.addAttribute("badTypeList", badType);
        model.addAttribute("badWordList", badWord);
        model.addAttribute("botRulesList", botRules);

        return "admin/chatbot";
    }

    @PostMapping("/chatbot")
    public String chatbot(Model model, String q, String sessId) {

        List<ChatbotBadTypeDTO> badType = chatbotRuleService.selectBadTypeList();
        List<ChatbotBadWordDTO> badWord = chatbotRuleService.selectBadWordList();
        List<ChatbotRulesDTO> botRules = chatbotRuleService.selectRulesList();

        model.addAttribute("badTypeList", badType);
        model.addAttribute("badWordList", badWord);
        model.addAttribute("botRulesList", botRules);

        String forbiddenResponse = chatbotRuleService.checkAllForbiddenWord(q);

        model.addAttribute("sessId", sessId);

        ChatbotHistDTO faHistDTO = new ChatbotHistDTO();
        faHistDTO.setBotType(2);
        faHistDTO.setBotSessId(sessId);

        if (forbiddenResponse != null){

            faHistDTO.setBotContent(forbiddenResponse);
            model.addAttribute("faHistDTO", faHistDTO);

            return "admin/chatbot";

        }

        faHistDTO.setBotContent("응답이 나오지 않습니다.");
        model.addAttribute("faHistDTO", faHistDTO);

        return "admin/chatbot";
    }

    @PostMapping("/forbidden")
    public String forbidden(Model model, ChatbotBadWordDTO badWordDTO) {


        badWordDTO.setBadUseYn("n");
        chatbotRuleService.insertBadWords(badWordDTO);

        List<ChatbotBadTypeDTO> badType = chatbotRuleService.selectBadTypeList();
        List<ChatbotBadWordDTO> badWord = chatbotRuleService.selectBadWordList();
        List<ChatbotRulesDTO> botRules = chatbotRuleService.selectRulesList();

        model.addAttribute("badTypeList", badType);
        model.addAttribute("badWordList", badWord);
        model.addAttribute("botRulesList", botRules);

        return "redirect:/admin/chatbot";
    }




    private String normalizeStatus(String status) {
        if ("pending".equalsIgnoreCase(status)) {
            return "pending";
        }
        if ("complete".equalsIgnoreCase(status)) {
            return "complete";
        }
        return "all";
    }




}