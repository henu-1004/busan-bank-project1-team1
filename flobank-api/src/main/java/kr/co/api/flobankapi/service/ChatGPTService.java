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
                1) 답변은 질문의 의도를 분명하게 파악하고, 불필요한 서론 없이 핵심만 간결하게 말해라.
                2) 숫자(금리, 기간, 금액)는 context 그대로 정확히 사용하라.
                3) 금리나 기간 목록이 여러 개일 경우, 조건(통화, 예치기간, 상품종류)을 먼저 설명하고 해당 값만 제시하라.
                4) 규정, 조항, 심의 내용은 문서의 표현 그대로 유지하되, 의미가 분명하게 전달되도록 재구성해라.
                5) context 칸에 아무것도 들어오지 않았을 경우, '참고하지 않았음' 첫 줄에 출력
                6) context 칸에 무언가 들어왔을 경우, 첫 줄에 그 내용 출력
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