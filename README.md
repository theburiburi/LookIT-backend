# LookIT Backend

AI 기반 가상 피팅 및 스타일 추천 플랫폼 **LookIT**의 백엔드 서버입니다.

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3 |
| Database | MySQL, Redis |
| Storage | AWS S3 |
| Auth | Kakao OAuth2, JWT |
| HTTP Client | WebClient (WebFlux) |
| Build | Gradle |

---

## 핵심 아키텍처 — Redis 분산 큐 기반 비동기 처리

AI 이미지 생성은 요청 1건당 약 30초가 소요되는 무거운 연산입니다. 다수의 사용자가 동시에 요청을 보낼 경우 서버 자원이 고갈될 수 있어, **요청 접수와 실제 연산을 분리하는 비동기 처리 구조**를 도입했습니다.

API 서버는 요청을 Redis List에 적재한 뒤 즉시 응답하고, Consumer 인스턴스들이 Redis 큐에서 작업을 하나씩 가져가 처리합니다. 여러 서버 인스턴스가 같은 Redis를 바라보더라도 `BRPOP`이 원자적으로 메시지를 가져오기 때문에 작업 중복을 줄이고, AI 서버와 DB에 들어가는 부하를 큐 속도로 평탄화할 수 있습니다.

```
클라이언트
    │
    ▼
[POST /api/v0/virtual-fitting]     ← 즉시 응답 (200 OK)
    │
    ▼
[FittingQueueProducer]              ← 이미지 Base64 인코딩 + JSON 직렬화
    │
    ▼
[Redis List: virtual_fitting_queue] ← LPUSH
    │
    ▼
[FittingConsumer]                   ← BRPOP, 분산 Consumer 확장 가능
    │
    ▼
[FittingProcessor]                  ← AI 서버 호출 → 결과 DB 저장
```

### Producer (`FittingQueueProducer`)
- 이미지를 Base64로 인코딩 후 JSON 직렬화하여 Redis List에 `LPUSH`
- 클라이언트는 큐 적재 즉시 응답을 받음
- 큐 적재 실패 시 서버 오류로 응답하여 요청 유실을 방지

### Consumer (`FittingConsumer`)
- 서버 시작 시 daemon 스레드로 실행 (`setDaemon(true)`)
- `BRPOP`(60초 timeout)으로 큐를 블로킹 대기 — 불필요한 폴링 방지
- Redis의 원자적 pop을 활용해 여러 Consumer 인스턴스가 같은 큐를 나누어 처리
- 예외 발생 시 로그만 남기고 루프를 유지하여 다음 메시지 처리 계속

### Processor (`FittingProcessor`)
- Consumer가 꺼낸 메시지를 이미지 바이트로 복원
- AI 서버를 호출하고 결과 URL을 `virtual_fitting` 테이블에 저장
- 큐 적재/소비/작업 실행 책임을 분리하여 트래픽 제어 로직과 비즈니스 저장 로직을 독립적으로 확장 가능

---

## 주요 기능

| 기능 | 설명 |
|------|------|
| 카카오 소셜 로그인 | OAuth2 인가 코드 방식, JWT 발급 |
| 신체/얼굴 분석 | AI 서버 연동, Redis 캐싱 |
| 스타일 추천 | 분석 결과 기반 브랜드/스타일 추천 |
| 가상 피팅 (비동기) | Redis 큐 기반 비동기 이미지 생성 |
| 가상 피팅 결과 조회/삭제 | S3 URL 반환, 소유권 검증 포함 |

---

## API 엔드포인트

### 가상 피팅
| Method | URI | 설명 |
|--------|-----|------|
| POST | `/api/v0/virtual-fitting` | 가상 피팅 요청 (비동기) |
| POST | `/api/v0/virtual-fitting/sync` | 가상 피팅 요청 (동기) |
| GET | `/api/v0/virtual-fitting/result` | 피팅 결과 목록 조회 |
| DELETE | `/api/v0/virtual-fitting` | 피팅 결과 삭제 |

### 인증
| Method | URI | 설명 |
|--------|-----|------|
| GET | `/api/v0/auth/kakao` | 카카오 로그인 |
| POST | `/api/v0/auth/sign-up` | 회원가입 |
| DELETE | `/api/v0/auth/withdraw` | 회원 탈퇴 |

---

## 환경 변수 (application.yml)

```yaml
spring:
  datasource:
    url: jdbc:mysql://...
  data:
    redis:
      host: localhost
      port: 6379

fitting:
  queue:
    consumer-timeout-seconds: 60

cloud:
  aws:
    s3:
      bucket: your-bucket-name
    credentials:
      access-key: YOUR_ACCESS_KEY
      secret-key: YOUR_SECRET_KEY

jwt:
  secret: YOUR_JWT_SECRET

kakao:
  client-id: YOUR_KAKAO_CLIENT_ID
```

---

## 로컬 실행

```bash
# Redis 실행 (Docker)
docker run -d -p 6379:6379 redis

# 빌드 및 실행
./gradlew bootRun
```
