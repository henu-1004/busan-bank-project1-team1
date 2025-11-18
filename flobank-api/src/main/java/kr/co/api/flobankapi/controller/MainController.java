package kr.co.api.flobankapi.controller;

import kr.co.api.flobankapi.service.BriefingService;
import kr.co.api.flobankapi.dto.BriefingViewDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import kr.co.api.flobankapi.service.BoardService;
import kr.co.api.flobankapi.dto.BoardDTO;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;





@Controller
@RequiredArgsConstructor
public class MainController {

    /**
     * 메인 페이지
     */
    private final BriefingService briefingService;
    private final BoardService boardService;



    @GetMapping("/")
    public String mainPage(
            @RequestParam(value = "mode", required = false, defaultValue = "oneday")
            String mode,
            Model model
    ) {

        BriefingViewDTO briefingView = briefingService.buildBriefingView(mode);

        model.addAttribute("briefingMode", briefingView.getBriefingMode());
        model.addAttribute("briefingTitle", briefingView.getBriefingTitle());
        model.addAttribute("briefingDateText", briefingView.getBriefingDateText());
        model.addAttribute("briefingLines", briefingView.getBriefingLines());

        /**  2) 공지사항 5개 */
        Map<String, Object> noticePage = boardService.getNoticePage(1);
        List<BoardDTO> noticeList = ((List<BoardDTO>) noticePage.get("list"))
                .stream()
                .limit(5)
                .collect(Collectors.toList());
        model.addAttribute("noticeList", noticeList);

        /**  3) 이벤트 5개 */
        Map<String, Object> eventPage = boardService.getEventPage(1);
        List<BoardDTO> eventList = ((List<BoardDTO>) eventPage.get("list"))
                .stream()
                .limit(5)
                .collect(Collectors.toList());
        model.addAttribute("eventList", eventList);

        return "index";
    }

}