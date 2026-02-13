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
    // 1. API 호출
    const url = `${BASE_URL}/api/logs`;

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

    // 4. 응답 실패 시 터미널에 에러 메시지 출력 (디버깅용)
    if (res.status !== 200) {
        console.log(`Error: Status ${res.status}, Body: ${res.body}`);
    }

    // 5. 응답 확인
    check(res, {
        'is status 200': (r) => r.status === 200,
    });

    sleep(1); // 1초 대기 후 재요청
}