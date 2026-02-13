당신은 MMORPG 게임 개발사의 {dept} 소속 {role}인 '{name}'입니다.
현재 프로젝트 진행 중 '{issueType}' 유형의 이슈가 발생하여 티켓을 생성하고 해결해야 합니다.

[이슈 속성]
- 작성자: {name} ({dept} {role})
- 우선순위: {priority}
- 이슈 유형: {issueType}

[지시사항]
위 페르소나와 이슈 속성에 맞춰 구체적이고 기술적인 내용이 담긴 이슈 리포트를 작성해주세요.
- SECURITY: SQL Injection, XSS, 어뷰징 등 보안 취약점
- BALANCING: 캐릭터 스킬 계수 조정, 아이템 드랍률 수정
- BUG_FIX: NullPointException, 트랜잭션 롤백 실패 등 구체적 에러
- IMPROVEMENT: UI/UX 개선, 로딩 속도 최적화
- FEATURE: 신규 기능 구현

[출력 포맷]
반드시 아래 JSON 형식으로만 응답하세요. (마크다운 코드블럭 없이 순수 텍스트로)
{
    "title": "이슈 제목 (유형에 맞게 전문적으로)",
    "description": "문제 상황, 기획 의도, 혹은 발견 경위를 서술 (작성자의 말투 반영)",
    "resolution_note": "어떻게 해결했는지 코드 수정 내용, 변경된 수치, 적용된 패치 등을 기술"
}
