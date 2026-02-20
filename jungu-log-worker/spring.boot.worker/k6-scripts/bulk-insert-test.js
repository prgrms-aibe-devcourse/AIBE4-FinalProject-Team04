import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
  stages: [
    { duration: '30s', target: 10 }, // 30초 동안 10명의 유저로 증가
    { duration: '1m', target: 10 },  // 1분 동안 10명의 유저 유지
    { duration: '30s', target: 0 },  // 30초 동안 0명으로 감소
  ],
};

export default function () {
  // Docker Compose 네트워크 내에서 'api' 서비스 이름으로 접근
  const url = 'http://api:8080/api/logs';

  // 100개의 로그 데이터를 생성
  const payload = JSON.stringify(Array.from({ length: 100 }, (_, i) => ({
    projectId: 'project-1',
    sessionId: `session-${i}`,
    userId: `user-${i}`,
    severity: 'INFO',
    body: `Log message ${i}`,
    fingerprint: `fingerprint-${i}`,
    resource: { service: 'test-service' },
    attributes: { 'http.method': 'GET' }
  })));

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  let res = http.post(url, payload, params);

  check(res, {
    'is status 202': (r) => r.status === 202,  // 202 Accepted
  });

  sleep(1);
}
