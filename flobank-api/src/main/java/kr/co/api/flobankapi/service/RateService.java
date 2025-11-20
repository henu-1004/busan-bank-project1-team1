package kr.co.api.flobankapi.service;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateService {

    private final RedisTemplate<String, String> redisTemplate;

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
}