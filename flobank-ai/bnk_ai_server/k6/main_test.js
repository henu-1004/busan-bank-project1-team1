import http from 'k6/http';
import { check, sleep } from 'k6';

// ------------------------
// 메인 페이지 부하 옵션
// ------------------------
export const options = {
  stages: [
    { duration: '2s', target: 8000 },  // 30초 동안 10명까지 증가
    { duration: '5s',  target: 1000 },  // 1분 동안 30명 유지 (메인 부하)
    { duration: '5s', target: 0 },   // 점진적 종료
  ],
};

// ------------------------
// 테스트 URL 설정
// ------------------------
const BASE_URL = 'http://34.64.124.33:8080/flobank/';

// ------------------------
// 테스트 함수
// ------------------------
export default function () {
  const res = http.get(BASE_URL);

  check(res, {
    'status 200': (r) => r.status === 200,
    'body not empty': (r) => r.body && r.body.length > 50,
  });

  sleep(1); // 사용자가 페이지 머무는 느낌
}
