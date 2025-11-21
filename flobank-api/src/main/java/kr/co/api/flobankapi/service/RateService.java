package kr.co.api.flobankapi.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper; // JSON 파싱용 (Spring Boot 기본 제공)

    @Value("${eximbank.api.base-url}")
    private String baseUrl;

    @Value("${eximbank.api.auth-key}")
    private String authKey;

    public String getRate(String date) {

        String searchDate = date.replace("-", "");
        String redisKey = "fx:" + searchDate;

        // Redis에서 먼저 조회
        String cached = redisTemplate.opsForValue().get(redisKey);
        if (cached != null) {
//            System.out.println("Redis HIT → " + redisKey);
            return cached;
        }

//        System.out.println("Redis MISS → Exim API 요청");

        // API 호출
        String url = baseUrl +
                "?authkey=" + authKey +
                "&searchdate=" + searchDate +
                "&data=AP01";

        RestTemplate rest = new RestTemplate();
        String response = rest.getForObject(url, String.class);

        // 영구 저장
        if (response != null) {
            redisTemplate.opsForValue().set(redisKey, response);  // TTL 없음
        }

        return response;
    }

    // 특정 통화의 환율만 추출
    public double getCurrencyRate(String currency) {
        // 1. 오늘 날짜 구하기 (주말/공휴일 처리는 별도 로직 필요하지만 일단 오늘로 시도)
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // 2. 전체 리스트 가져오기 (기존 메서드 활용 -> Redis 자동 사용됨)
        String jsonResponse = getRate(today);

        if (jsonResponse == null || jsonResponse.isEmpty() || jsonResponse.equals("[]")) {
            // 오늘 데이터가 없으면(공휴일 등) 어제나 최근 데이터를 찾는 로직이 필요할 수 있음
            // 일단 테스트를 위해 0.0 반환
            return 0.0;
        }

        // 3. JSON 파싱해서 해당 통화 찾기
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);

            if (root.isArray()) {
                for (JsonNode node : root) {
                    // cur_unit이 일치하는지 확인 (JPY(100) 같은 경우 처리 필요)
                    String curUnit = node.path("cur_unit").asText();

                    // JPY(100), IDR(100) 등은 괄호 제거하고 비교하거나 포함 여부 확인
                    if (curUnit.equals(currency) || curUnit.startsWith(currency + "(")) {

                        // "deal_bas_r" (매매기준율) 가져오기. 콤마(,) 제거 후 파싱
                        String dealBasR = node.path("kftc_deal_bas_r").asText().replace(",", "");
                        // 만약 kftc_deal_bas_r이 없으면 deal_bas_r 사용
                        if(dealBasR.isEmpty() || dealBasR.equals("0")) {
                            dealBasR = node.path("deal_bas_r").asText().replace(",", "");
                        }

                        return Double.parseDouble(dealBasR);
                    }
                }
            }
        } catch (Exception e) {
            log.error("JSON 파싱 에러", e);
        }

        return 0.0;
    }
}