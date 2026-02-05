# 코드 컨벤션

## 1. 📝 명명 규칙

### 1.1 일반 원칙
- **패키지:** 소문자만 사용 (언더스코어 사용 금지)
    - `com.project.domain.member` ⭕
    - `com.project.domain_member` ❌
- **클래스:** PascalCase, 명사 사용
    - `MemberService`, `OrderPayment`
- **메서드/변수:** camelCase, 동사/명사 사용
    - `findMember()`, `totalCount`
- **상수:** UPPER_SNAKE_CASE
    - `MAX_LOGIN_RETRY`, `DEFAULT_PAGE_SIZE`

### 1.2 Java Class
- **API Controller:** `도메인 + ApiController` (`MemberApiController`)
- **View Controller:** `도메인 + ViewController` (`MemberViewController`)
- **Service/Repository:** `MemberService`, `MemberRepository`
- **Entity:** `Member` (DB 테이블명과 일치)

### 1.3 Thymeleaf (HTML)
- **파일명:** kebab-case 사용
    - `member-list.html` ⭕
    - `memberList.html` ❌
- **폴더 구조:** 도메인별 디렉토리 분리
    - `resources/templates/member/join-form.html`

### 1.4 URL
- **API URL:** `/api/` 접두사 필수
    - `GET /api/members/{id}`
- **View URL:** 접두사 없음
    - `GET /members/join`

## 2. 🏗 레이어별 작성 규칙

### 2.1 Controller

#### API Controller (`@RestController`)
- 반환값: JSON (`ResponseEntity<Dto>`)
- **Entity 직접 반환 금지**
```java
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberApiController {
    private final MemberService memberService;

    @PostMapping
    public ResponseEntity<Long> join(@RequestBody MemberJoinRequest request) {
        return ResponseEntity.ok(memberService.join(request));
    }
}
```

#### View Controller (`@Controller`)
- 반환값: String (HTML 경로)
- `Model`로 데이터 전달
```java
@Controller
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberViewController {
    
    @GetMapping("/join")
    public String joinForm(Model model) {
        model.addAttribute("form", new MemberJoinRequest());
        return "member/join-form";
    }
}
```

### 2.2 Entity (JPA)
- **`@Data` 금지:** `@Getter`만 사용
- **기본 생성자 보호:** `@NoArgsConstructor(access = AccessLevel.PROTECTED)`
- **Setter 지양:** 의미 있는 메서드로 대체
```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String password;

    public void changePassword(String newPassword) {
        this.password = newPassword;
    }
}
```

### 2.3 Service
- **트랜잭션:** 클래스 레벨 `@Transactional(readOnly = true)`, 쓰기 메서드만 `@Transactional`
- **DTO 변환:** Service에서 Entity ↔ DTO 변환 수행

## 3. 🌐 API URL 설계

### 3.1 기본 원칙
- **View (HTML):** `/api` 접두사 사용 안 함
- **API (JSON):** `/api` 접두사 필수
- URL은 **자원(Resource)** 표현 (행위 포함 금지)

### 3.2 URL 예시

| 기능 | HTTP Method | View URL | API URL |
|------|-------------|----------|---------|
| 목록 조회 | GET | `/members` | `/api/members` |
| 단건 조회 | GET | `/members/1` | `/api/members/1` |
| 등록 | POST | - | `/api/members` |
| 수정 | PUT/PATCH | - | `/api/members/1` |
| 삭제 | DELETE | - | `/api/members/1` |
| 로그인 페이지 | GET | `/login` | - |

### 3.3 안티패턴 (Bad Practice)
- View URL에 `/api` 붙이기: `GET /api/members/login` ❌
- API URL에 `/api` 빼먹기: `POST /members` (JSON 반환) ❌
- 행위 포함: `/api/getMembers`, `/api/createMember` ❌