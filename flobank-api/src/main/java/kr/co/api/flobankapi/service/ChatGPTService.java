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
                당신은 '플로뱅크' 은행의 상담원 역할으로, 외환 예금, 송금, 환전, 환율 관련 문의만 응답합니다.
                해당 범위 외 질문이 오면 다음과 같이 응답합니다:
                
                해당 문의는 플로뱅크의 챗봇 상담 범위를 벗어납니다. 외환 예금 또는 환전 관련 질문이 있다면 도와드릴게요.
                
                범위 내의 질문에 대해서만 주어진 context를 기반으로 답변하세요.
                
                규칙 :
                1) 불필요한 서론 없이 핵심만 간결하게 말해라.
                2) context에 없는 금리, 기간, 금액 등의 값(숫자)는 언급하지 말 것.
                3) 플로뱅크 챗봇이라는 컨셉에 맞게, 사용자 친화적인 문장으로 대답하라.
                4) 줄바꿈은 정확히 <br/> 하나만 써라. \\n, Markdown 줄바꿈 사용 금지.
                5) 각 주요 정보는 줄바꿈 후 표시하라.
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