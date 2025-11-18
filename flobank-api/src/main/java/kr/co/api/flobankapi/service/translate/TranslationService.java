package kr.co.api.flobankapi.service.translate;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TranslationService {
    @Value("${deepl.api.key}")
    private String deepLApiKey;

    private final RedisTemplate<String, String> redisTemplate;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api-free.deepl.com/v2")
            .build();

    public String translate(String text, String targetLang) {

        // 캐시 Key 생성
        String cacheKey = "deepl:" + targetLang.toLowerCase() + ":" + sha256(text);

        // Redis 캐시 조회
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            System.out.println("Redis Cache Hit!");
            return cached;
        }

        System.out.println(" DeepL API 호출...");

        // ------------------ [수정 시작] ------------------

        // ✅ 1. DeepL API 요청 Body (스펙에 맞게 수정)
        Map<String, Object> body = Map.of(
                // "auth_key" 필드 제거
                "text", List.of(text), // 🚨 text를 List<String>으로 변경
                "target_lang", targetLang.toUpperCase()
        );

        // DeepL 호출
        String translated = webClient.post()
                .uri("/translate")
                // ✅ 2. 인증 키를 Body가 아닌 Header로 전송
                .header("Authorization", "DeepL-Auth-Key " + deepLApiKey)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(res -> {
                    List<Map<String, String>> t = (List<Map<String, String>>) res.get("translations");
                    return t.get(0).get("text");
                })
                .block();

        // ------------------ [수정 끝] ------------------


        // Redis에 캐시 저장 (12시간 유지)
        redisTemplate.opsForValue().set(cacheKey, translated, 12, TimeUnit.HOURS);

        return translated;
    }

    // 문자열을 SHA-256 해시값으로 바꿈

    private String sha256(String base) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
