package kr.co.api.flobankapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class QnaAiService {

    @Value("${ai-server-qna.url}")
    private String qnaAiUrl;

    private final WebClient webClient;

    public void sendToAi(Long qnaNo, String question, String title) {
        if (qnaNo == null) {
            log.warn("[QNA-AI] Skip sending because qnaNo is null");
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("qnaNo", qnaNo);
        payload.put("question", question);
        payload.put("title", title);

        try {
            webClient.post()
                    .uri(qnaAiUrl)
                    .bodyValue(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .doOnSuccess(res -> log.info("[QNA-AI] Sent to AI server qnaNo={} status={}", qnaNo, res.getStatusCode()))
                    .doOnError(err -> log.error("[QNA-AI] Failed to call AI server for qnaNo={} reason={} ", qnaNo, err.getMessage()))
                    .block();
        } catch (Exception e) {
            log.error("[QNA-AI] Exception while calling AI server for qnaNo={}: {}", qnaNo, e.getMessage());
        }
    }
}
