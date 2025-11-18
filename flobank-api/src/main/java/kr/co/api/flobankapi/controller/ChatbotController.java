package kr.co.api.flobankapi.controller;

import kr.co.api.flobankapi.dto.ChatbotHistDTO;
import kr.co.api.flobankapi.dto.ChatbotSessionDTO;
import kr.co.api.flobankapi.dto.SearchResDTO;
import kr.co.api.flobankapi.jwt.CustomUserDetails;
import kr.co.api.flobankapi.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatbotController {

    private final QTypeClassifierService typeClassifier;
    private final EmbeddingService embeddingService;
    private final PineconeService pineconeService;
    private final ChatGPTService chatGPTService;
    private final ChatbotSessionService chatbotSessionService;
    private final ChatbotHistService chatbotHistService;
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @GetMapping("/mypage/chatbot")
    public String chatbot(@AuthenticationPrincipal CustomUserDetails user, Model model) {

        ChatbotSessionDTO sessDTO = new ChatbotSessionDTO();
        if (user != null) {
            sessDTO.setSessCustCode(user.getUsername());
        }
        sessDTO.setSessStartDt(LocalDateTime.now().format(formatter));

        sessDTO = chatbotSessionService.insertSess(sessDTO);
        model.addAttribute("sessId", sessDTO.getSessId());

        return "mypage/chatbot";
    }

    @PostMapping("/mypage/chatbot")
    public String chatbot(Model model, String q, String sessId) {
        System.out.println("GPT API 호출 들어옴 = " + System.currentTimeMillis());

        ChatbotHistDTO qHistDTO = new ChatbotHistDTO();
        qHistDTO.setBotType(1);
        qHistDTO.setBotContent(q);
        qHistDTO.setBotSessId(sessId);
        chatbotHistService.insertHist(qHistDTO);


        try {
            // 문서 타입 자동 분류
            String type = typeClassifier.detectTypeByGPT(q);
            System.out.println("=== 자동 분류된 TYPE = " + type);

            // 질문 임베딩
            List<Double> qEmbedding = embeddingService.embedText(q);
            log.info(">>> 벡터 : " + qEmbedding);

            // Pinecone search
            var results = pineconeService.search(
                    qEmbedding,
                    10,          // topK
                    "fx-interest",       // namespace 전체 검색
                    type,       // GPT가 판별한 문서 타입 (null 가능)
                    0        // 최소 유사도 컷
            );

            // 검색된 문서로 문맥 텍스트 구성
            StringBuilder contextBuilder = new StringBuilder();
            for (SearchResDTO r : results) {
                Map<String, Object> meta = r.getMetadata();
                if (meta.containsKey("content")) {
                    contextBuilder.append(meta.get("content")).append("\n\n");
                }
            }
            String context = contextBuilder.toString();
            log.info("=== 최종 context ===\n" + context);

            // GPT 호출 (문맥 + 질문)
            String response = chatGPTService.ask(q, context);

            ChatbotHistDTO aHistDTO = new ChatbotHistDTO();
            aHistDTO.setBotType(2);
            aHistDTO.setBotContent(response);
            aHistDTO.setBotSessId(sessId);
            chatbotHistService.insertHist(aHistDTO);

            List<ChatbotHistDTO> dtoList = chatbotHistService.selectHist(sessId);

            model.addAttribute("dtoList", dtoList);
            model.addAttribute("response", response);
            model.addAttribute("sessId", sessId);

            return "mypage/chatbot";

        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "mypage/chatbot";
        }
    }
}
