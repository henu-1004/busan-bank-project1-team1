package kr.co.api.flobankapi.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class RateService {

    @Value("${eximbank.api.base-url}")
    private String baseUrl;

    @Value("${eximbank.api.auth-key}")
    private String authKey;

    public String getRate(String date) {

        // 2025-11-04 → 20251104 변환
        String searchDate = date.replace("-", "");

        String url = baseUrl +
                "?authkey=" + authKey +
                "&searchdate=" + searchDate +
                "&data=AP01";

        RestTemplate rest = new RestTemplate();

        // JSON 문자열 그대로 반환
        return rest.getForObject(url, String.class);
    }





}
