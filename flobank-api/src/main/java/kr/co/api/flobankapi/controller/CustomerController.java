package kr.co.api.flobankapi.controller;

import kr.co.api.flobankapi.dto.FaqDTO;
import kr.co.api.flobankapi.service.FaqService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import kr.co.api.flobankapi.dto.BoardDTO;
import kr.co.api.flobankapi.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

import kr.co.api.flobankapi.dto.FaqDTO;
import kr.co.api.flobankapi.service.FaqService;







@Controller
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final BoardService boardService;
    private final FaqService faqService;



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

        // 카테고리별 그룹핑
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

        model.addAttribute("noticeList", noticePage.get("list"));  // ← 수정됨
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


    @GetMapping("/qna_edit")
    public String qna_edit(Model model){
        model.addAttribute("activeItem","qna");
        return "customer/qna_edit";
    }

    @GetMapping("/qna_list")
    public String qna_list(Model model){
        model.addAttribute("activeItem","qna");
        return "customer/qna_list";
    }

    @GetMapping("/qna_view")
    public String qna_view(Model model){
        model.addAttribute("activeItem","qna");
        return "customer/qna_view";
    }

    @GetMapping("/qna_write")
    public String qna_write(Model model){
        model.addAttribute("activeItem","qna");
        return "customer/qna_write";
    }

}
