import http from 'k6/http';
import { check, sleep } from 'k6';

// 부하 옵션
export const options = {
    vus: 20,          // 동시 사용자 20명
    duration: '30s',  // 30초 동안 부하
};

// 테스트할 스프링 API 주소
const BASE_URL = 'http://34.64.124.33:8080/flobank/'; 
// 예: 'http://34.64.124.33:8080/flobank/customer/index'

export default function () {
    const res = http.get(BASE_URL);

    check(res, {
        'status is 200': (r) => r.status === 200,
    });

    sleep(1); // 1초 쉬고 다시 요청
}
