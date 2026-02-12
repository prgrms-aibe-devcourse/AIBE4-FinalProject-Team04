import http from 'k6/http';
import { check, sleep } from 'k6';

// 테스트 설정 (부하 수준)
export const options = {
    stages: [
        { duration: '10s', target: 100 }, // 10초 동안 유저 100명까지 서서히 증가
        { duration: '20s', target: 100 }, // 20초 동안 유저 100명 유지
        { duration: '10s', target: 0 },  // 10초 동안 유저 0명으로 감소 (종료)
    ],
};

// 실행 시 전달받은 BASE_URL을 사용 (기본값은 로컬용 api 서비스명)
const BASE_URL = __ENV.BASE_URL || 'http://api:8080';

export default function () {
    // 1. API 호출 (Docker 내부 통신이므로 서비스명 'api' 사용 가능하지만,
    // 로컬 테스트의 확실함을 위해 host.docker.internal 또는 내부 IP 사용 권장.
    // 여기서는 Docker Compose 네트워크 내부 DNS인 'api'를 사용)
    const url = 'http://api:8080/api/logs';

    // 2. 보낼 데이터 (JSON)
    const payload = JSON.stringify({
        level: "INFO",
        message: "Test log message from k6",
        service: "payment-service"
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    // 3. POST 요청 전송
    const res = http.post(url, payload, params);

    // 4. 응답 확인 (200 OK가 왔는지?)
    check(res, {
        'is status 200': (r) => r.status === 200,
    });

    sleep(1); // 1초 대기 후 재요청
}