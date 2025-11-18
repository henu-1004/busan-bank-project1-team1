package kr.co.api.flobankapi.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import kr.co.api.flobankapi.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import kr.co.api.flobankapi.dto.BoardDTO;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import kr.co.api.flobankapi.service.FaqService;
import kr.co.api.flobankapi.dto.FaqDTO;
import java.util.List;

import java.util.Map;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final BoardService boardService;
    private final FaqService faqService;



    /** 관리자 대시보드 */
    @GetMapping("/index")
    public String index(Model model) {
        model.addAttribute("activeItem", "dashboard");
        return "admin/index";
    }

    /** 목록 */
    @GetMapping("/member")
    public String member(@RequestParam(defaultValue = "1") int page,
                         @RequestParam(name = "faqPage", defaultValue = "1") int faqPage,
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

        model.addAttribute("activeItem", "member");

        return "admin/member";
    }





    /** 외화예금 상세 보기 */
    @GetMapping("/products_view")
    public String productsView(Model model) {
        model.addAttribute("activeItem", "products");
        return "admin/products_view";
    }

    /** 환전 관리 */
    @GetMapping("/exchange")
    public String exchange(Model model) {
        model.addAttribute("activeItem", "exchange");
        return "admin/exchange";
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






}
