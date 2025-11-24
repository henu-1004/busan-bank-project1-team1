package kr.co.api.flobankapi.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

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
    public Map<String, Double> getCurrencyRate(String currency) {
        // 1. 조회 시작 날짜 계산 (토/일/월 오전 -> 금요일 등)
        LocalDate targetDate = getTargetDate();
        String formattedDate = targetDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        String jsonResponse = null;

        // 2. 데이터가 없으면(공휴일 등) 최대 5일 전까지 과거로 가며 데이터를 찾음 (안전장치)
        for (int i = 0; i < 5; i++) {
            jsonResponse = getRate(formattedDate);

            // 데이터가 유효하면 반복 종료
            if (jsonResponse != null && !jsonResponse.isEmpty() && !jsonResponse.equals("[]")) {
                break;
            }

            // 데이터가 없으면 하루 전으로 이동하여 재시도
            targetDate = targetDate.minusDays(1);
            formattedDate = targetDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        }

        // 5일간 뒤져도 없으면 null 반환
        if (jsonResponse == null || jsonResponse.isEmpty() || jsonResponse.equals("[]")) {
            return null;
        }

        // 3. JSON 파싱
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            if (root.isArray()) {
                for (JsonNode node : root) {
                    String curUnit = node.path("cur_unit").asText();

                    // JPY(100), IDR(100) 등 괄호 처리 포함
                    if (curUnit.equals(currency) || curUnit.startsWith(currency + "(")) {

                        // (1) 매매기준율 (deal_bas_r) 파싱
                        String dealBasR = node.path("kftc_deal_bas_r").asText().replace(",", "");
                        if (dealBasR.isEmpty() || dealBasR.equals("0")) {
                            dealBasR = node.path("deal_bas_r").asText().replace(",", "");
                        }

                        // (2) 전신환매도율 (tts) 파싱 (보내실 때 환율 - 고객 매수)
                        String tts = node.path("tts").asText().replace(",", "");

                        // (3) 전신환매입율 (ttb) 파싱 (받으실 때 환율 - 고객 매도)
                        String ttb = node.path("ttb").asText().replace(",", "");

                        // Map에 담아서 반환
                        Map<String, Double> result = new HashMap<>();
                        result.put("rate", dealBasR.isEmpty() ? 0.0 : Double.parseDouble(dealBasR));
                        result.put("tts", tts.isEmpty() ? 0.0 : Double.parseDouble(tts));
                        result.put("ttb", ttb.isEmpty() ? 0.0 : Double.parseDouble(ttb)); // ✅ TTB 추가

                        return result;
                    }
                }
            }
        } catch (Exception e) {
            log.error("JSON 파싱 에러", e);
        }

        return null;
    }

    // 영업일 기준 날짜 계산 로직
    private LocalDate getTargetDate() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate date = now.toLocalDate();
        DayOfWeek dayOfWeek = now.getDayOfWeek();
        int hour = now.getHour();

        // 1. 토요일이면 -> 1일 전(금)
        if (dayOfWeek == DayOfWeek.SATURDAY) {
            return date.minusDays(1);
        }
        // 2. 일요일이면 -> 2일 전(금)
        else if (dayOfWeek == DayOfWeek.SUNDAY) {
            return date.minusDays(2);
        }
        // 3. 월요일이고 11시 이전이면 -> 3일 전(금)
        else if (dayOfWeek == DayOfWeek.MONDAY && hour < 11) {
            return date.minusDays(3);
        }
        // 4. 화~금 평일인데 11시 이전이면 -> 1일 전 (아직 오늘 고시 안됨)
        else if (hour < 11) {
            return date.minusDays(1);
        }

        // 그 외(평일 11시 이후)는 오늘 날짜 반환
        return date;
    }
}