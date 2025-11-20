package kr.co.api.flobankapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;



@Service
@RequiredArgsConstructor
public class ChatGPTService {

    private final WebClient openAiWebClient;
    private final ObjectMapper objectMapper;

    public String ask(String question, String context) {

        String systemPrompt =
                """
                너는 '플로뱅크' 금융 문서 기반 RAG 챗봇이다.
                주어진 context를 기반으로 답변하라.
                
                규칙 :
                1) 불필요한 서론 없이 핵심만 간결하게 말해라.
                2) context에 없는 금리, 기간, 금액 등의 값(숫자)는 언급하지 말 것.
                3) 플로뱅크 챗봇이라는 컨셉에 맞게, 사용자 친화적인 문장으로 대답하라.
                4) 줄바꿈은 정확히 <br/> 하나만 써라. \\n, Markdown 줄바꿈 사용 금지.
                5) 각 주요 정보는 줄바꿈 후 표시하라.
                6) 정보가 미제시되거나 없다고 말하지 말 것
                """;

        String userPrompt =
                """
                ====== context ======
                %s
    
                ====== question ======
                %s
                """
                        .formatted(context, question);

        String responseJson = openAiWebClient.post()
                .uri("/chat/completions")
                .bodyValue(Map.of(
                        "model", "gpt-4o-mini",
                        "messages", List.of(
                                Map.of("role", "system", "content", systemPrompt),
                                Map.of("role", "user", "content", userPrompt)
                        ),
                        "max_tokens", 1024,
                        "temperature", 0
                ))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        try {
            JsonNode root = objectMapper.readTree(responseJson);
            return root
                    .path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();
        } catch (Exception e) {
            throw new RuntimeException("응답 파싱 실패: " + e.getMessage());
        }
    }

}